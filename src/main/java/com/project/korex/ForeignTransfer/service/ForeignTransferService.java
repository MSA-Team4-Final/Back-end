package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.request.ForeignTransferRequest;
import com.project.korex.ForeignTransfer.dto.request.TransferExchangeRequest;
import com.project.korex.ForeignTransfer.dto.response.ForeignTransferResponse;
import com.project.korex.ForeignTransfer.dto.response.TransferExchangeResponse;
import com.project.korex.ForeignTransfer.entity.*;
import com.project.korex.ForeignTransfer.enums.RequestStatus;
import com.project.korex.ForeignTransfer.enums.TransferStatus;
import com.project.korex.ForeignTransfer.repository.ForeignTransferTransactionRepository;
import com.project.korex.ForeignTransfer.repository.TermsAgreementRepository;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.AccountType;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ForeignTransferService {

    private final UserJpaRepository userRepository;
    private final ForeignTransferTransactionRepository transactionRepository;
    private final TermsAgreementRepository termsRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository globalTransactionRepository;
    private final CurrencyRepository currencyRepository;
    private final FileUploadService fileUploadService;
    private final ForeignTransferExchangeService foreignTransferExchangeService;
    private final TransferFeeAdminService feeAdminService;
    private final PasswordEncoder passwordEncoder; // 🔹 추가

    private final Set<String> supportedCurrencies = Set.of("USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY");

    @Transactional
    public ForeignTransferResponse processFullForeignTransfer(
            String loginId,
            ForeignTransferRequest request,
            MultipartFile idFile,
            MultipartFile proofDocumentFile,
            MultipartFile relationDocumentFile
    ) {
        Users user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

//        if (!passwordEncoder.matches(request.getTransactionPassword(), user.getTransactionPassword())) {
//            throw new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다");
//        }

        BigDecimal transferAmount = request.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("송금 금액이 유효하지 않습니다.");

        AccountType recipientAccountType = request.getAccountType() != null ? request.getAccountType() : AccountType.KRW;

        // 프론트에서 들어오는 수취통화 우선 (DTO 필드명은 프로젝트에 맞게 사용)
        String toCurrency = request.getRecipientCurrencyCode() != null && !request.getRecipientCurrencyCode().isEmpty()
                ? request.getRecipientCurrencyCode()
                : request.getCurrencyCode();

        if (toCurrency == null || toCurrency.isBlank()) {
            throw new RuntimeException("수취 통화가 유효하지 않습니다.");
        }

        // -----------------------------
        // 송금 계좌 기준 통화와 잔액 조회
        // -----------------------------
        String fromCurrencyCode;
        Balance sendingBalance;

        if (recipientAccountType == AccountType.KRW) {
            fromCurrencyCode = "KRW";
            sendingBalance = balanceRepository.findByUserIdAndAccountType(user.getId(), AccountType.KRW)
                    .orElseThrow(() -> new RuntimeException("원화 계좌가 없습니다."));
        } else {
            // 외화 계좌 선택: 송금 계좌 통화는 수취통화와 동일(USD 계좌에서 USD 보냄)
            fromCurrencyCode = toCurrency;
            sendingBalance = balanceRepository.findByUserIdAndCurrency_Code(user.getId(), toCurrency)
                    .orElseThrow(() -> new RuntimeException(toCurrency + " 외화 계좌가 없습니다."));
        }

        // -----------------------------
        // 환전 계산
        // -----------------------------
        TransferExchangeRequest exchangeRequest = TransferExchangeRequest.builder()
                .fromCurrency(fromCurrencyCode)
                .toCurrency(toCurrency)
                .amount(transferAmount)
                .accountType(recipientAccountType)
                .build();

        TransferExchangeResponse exchangeResult = foreignTransferExchangeService.executeExchange(exchangeRequest);

        // initial values from calculation
        BigDecimal convertedAmount = exchangeResult.getToAmount();
        BigDecimal feeAmount = exchangeResult.getFee(); // KRW 기준 수수료
        BigDecimal totalDeductedAmountKRW = exchangeResult.getTotalDeductedAmountKRW();

        // -----------------------------
        // 송금 계좌 잔액 차감 (송금 통화 기준)
        // -----------------------------
        BigDecimal amountToDeduct = exchangeResult.getTotalDeductedAmount(); // fromCurrency 기준 차감
        if (sendingBalance.getAvailableAmount().compareTo(amountToDeduct) < 0) {
            throw new RuntimeException("잔액 부족: " + fromCurrencyCode);
        }
        sendingBalance.setAvailableAmount(sendingBalance.getAvailableAmount().subtract(amountToDeduct));
        sendingBalance.setHeldAmount(sendingBalance.getHeldAmount().add(amountToDeduct));
        balanceRepository.save(sendingBalance);

        // -----------------------------
        // 외화 계좌인 경우 원화 수수료는 원화계좌에서 차감
        // -----------------------------
        if (!"KRW".equalsIgnoreCase(fromCurrencyCode)) {
            Balance krwBalance = balanceRepository.findByUserIdAndAccountType(user.getId(), AccountType.KRW)
                    .orElseThrow(() -> new RuntimeException("원화 계좌가 없습니다."));
            if (krwBalance.getAvailableAmount().compareTo(feeAmount) < 0) {
                throw new RuntimeException("원화 수수료 부족");
            }
            krwBalance.setAvailableAmount(krwBalance.getAvailableAmount().subtract(feeAmount));
            krwBalance.setHeldAmount(krwBalance.getHeldAmount().add(feeAmount));
            balanceRepository.save(krwBalance);
        }

        // -----------------------------
        // 글로벌 트랜잭션 기록 (fromCurrency = 송금계좌 통화, toCurrency = 수취 통화)
        // -----------------------------
        BigDecimal sendReceiveAmount = (recipientAccountType == AccountType.FOREIGN) ? convertedAmount : transferAmount;

        Transaction generalTransaction = Transaction.builder()
                .fromUser(user)
                .toUser(user)
                .transactionType(TransactionType.TRANSFER)
                .sendAmount(sendReceiveAmount)
                .receiveAmount(sendReceiveAmount)
                // 일단 임시로 set (나중에 강제 덮어쓰기)
                .exchangeRateApplied(BigDecimal.ZERO)
                .feeAmount(feeAmount)
                .totalDeductedAmount(totalDeductedAmountKRW)
                .fromCurrencyCode(currencyRepository.findByCode(fromCurrencyCode).orElseThrow())
                .toCurrencyCode(currencyRepository.findByCode(toCurrency).orElseThrow())
                .status("PENDING")
                .build();
        globalTransactionRepository.save(generalTransaction);

        // -----------------------------
        // ForeignTransferTransaction 저장
        // -----------------------------
        ForeignTransferTransaction ftTransaction = ForeignTransferTransaction.builder()
                .user(user)
                .transaction(generalTransaction)
                .requestStatus(RequestStatus.SUBMITTED)
                .transferStatus(TransferStatus.NOT_STARTED)
                .createdAt(LocalDateTime.now())
                .transferAmount(transferAmount)
                .convertedAmount(convertedAmount)
                .exchangeRate(BigDecimal.ZERO) // 나중에 강제 덮어쓰기
                .krwNumber(recipientAccountType == AccountType.KRW ? request.getAccountNumber() : null)
                .foreignNumber(recipientAccountType == AccountType.FOREIGN ? request.getAccountNumber() : null)
                .staffMessage(request.getStaffMessage())
                .relationRecipient(request.getRelationRecipient())
                .transactionType(TransactionType.TRANSFER)
                .build();

        RecipientSnapshot snapshot = new RecipientSnapshot();
        snapshot.setName(request.getRecipientName());
        snapshot.setBankName(request.getRecipientBank());
        snapshot.setAccountNumber(request.getRecipientAccountNumber());
        snapshot.setEmail(request.getRecipientEmail());
        snapshot.setTransaction(ftTransaction);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setCurrencyCode(toCurrency); // 확실히 수취 통화로 저장
        snapshot.setPhoneNumber(request.getPhoneNumber());
        snapshot.setEngAddress(request.getEngAddress());
        snapshot.setCountry(request.getCountry());
        snapshot.setCountryNumber(request.getCountryNumber());
        snapshot.setId(request.getRecipientId());

        Sender sender = Sender.builder()
                .user(user)
                .foreignTransferTransaction(ftTransaction)
                .name(request.getSenderName())
                .transferReason(request.getTransferReason())
                .countryNumber(request.getCountryNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .country(request.getCountry())
                .engAddress(request.getEngAddress())
                .relationRecipient(request.getRelationRecipient())
                .accountType(recipientAccountType.name())
                .accountNumber(request.getAccountNumber())
                .idFilePath(saveFilePath(ftTransaction, idFile, "ID"))
                .proofDocumentFilePath(saveFilePath(ftTransaction, proofDocumentFile, "PROOF"))
                .relationDocumentFilePath(saveFilePath(ftTransaction, relationDocumentFile, "RELATION"))
                .build();

        ftTransaction.setRecipientSnapshot(snapshot);
        ftTransaction.setSender(sender);
        transactionRepository.save(ftTransaction);

        TermsAgreement agreement = TermsAgreement.builder()
                .foreignTransferTransaction(ftTransaction)
                .agree1(request.isAgree1())
                .agree2(request.isAgree2())
                .agree3(request.isAgree3())
                .agreedAt(LocalDateTime.now())
                .build();
        termsRepository.save(agreement);

        // -----------------------------
        // 수취 통화 기준 환율로 강제 덮어쓰기(항상 적용)
        // - getExchangeRateSafe 사용해서 KRW 예외 방지
        // -----------------------------
        BigDecimal forcedRate = foreignTransferExchangeService.getExchangeRateSafe(toCurrency);

        // 덮어쓰기
        generalTransaction.setExchangeRateApplied(forcedRate);
        ftTransaction.setExchangeRate(forcedRate);

        // 필요 시 convertedAmount 강제 재계산
        BigDecimal finalConvertedAmount;
        if ("KRW".equalsIgnoreCase(fromCurrencyCode) && !"KRW".equalsIgnoreCase(toCurrency)) {
            // KRW -> 외화 (원래 계산과 동일하지만 round 조정)
            if ("JPY".equalsIgnoreCase(toCurrency)) {
                finalConvertedAmount = transferAmount.multiply(BigDecimal.valueOf(100)).divide(forcedRate, 0, RoundingMode.HALF_UP);
            } else {
                finalConvertedAmount = transferAmount.divide(forcedRate, 2, RoundingMode.HALF_UP);
            }
        } else if (!"KRW".equalsIgnoreCase(fromCurrencyCode) && "KRW".equalsIgnoreCase(toCurrency)) {
            // 외화 -> KRW (외화계좌에서 보내는 경우)
            if ("JPY".equalsIgnoreCase(fromCurrencyCode)) {
                finalConvertedAmount = transferAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(forcedRate).setScale(2, RoundingMode.HALF_UP);
            } else {
                finalConvertedAmount = transferAmount.multiply(forcedRate).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            // 동일 통화 (외화->외화 또는 KRW->KRW)
            finalConvertedAmount = exchangeResult.getToAmount();
        }

        ftTransaction.setConvertedAmount(finalConvertedAmount);
        transactionRepository.save(ftTransaction);
        globalTransactionRepository.save(generalTransaction);

        // -----------------------------
        // 최종 응답
        // -----------------------------
        return ForeignTransferResponse.builder()
                .transferId(ftTransaction.getId())
                .senderId(sender.getId())
                .accountType(recipientAccountType.name())
                .transferAmount(transferAmount)
                .convertedAmount(finalConvertedAmount)
                .appliedRate(forcedRate)
                .feeAmount(feeAmount)
                .transferReason(request.getTransferReason())
                .relationRecipient(request.getRelationRecipient())
                .requestStatus(ftTransaction.getRequestStatus().name())
                .transferStatus(ftTransaction.getTransferStatus().name())
                .createdAt(ftTransaction.getCreatedAt())
                .agreedAt(agreement.getAgreedAt())
                .build();
    }

    private String saveFilePath(ForeignTransferTransaction transaction, MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) return null;
        return fileUploadService.uploadFileToTransaction(transaction, file, fileType).getFileUrl();
    }
}
