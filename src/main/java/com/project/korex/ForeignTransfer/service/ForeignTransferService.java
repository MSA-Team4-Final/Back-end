package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.request.ForeignTransferRequest;
import com.project.korex.ForeignTransfer.dto.request.TransferExchangeRequest;
import com.project.korex.ForeignTransfer.dto.response.ForeignTransferResponse;
import com.project.korex.ForeignTransfer.dto.response.TransferExchangeResponse;
import com.project.korex.ForeignTransfer.entity.ForeignTransferTransaction;
import com.project.korex.ForeignTransfer.entity.RecipientSnapshot;
import com.project.korex.ForeignTransfer.entity.Sender;
import com.project.korex.ForeignTransfer.entity.TermsAgreement;
import com.project.korex.ForeignTransfer.enums.RequestStatus;
import com.project.korex.ForeignTransfer.enums.TransferStatus;
import com.project.korex.ForeignTransfer.repository.ForeignTransferTransactionRepository;
import com.project.korex.ForeignTransfer.repository.TermsAgreementRepository;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.AccountType;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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

        BigDecimal transferAmountKRW = request.getTransferAmount();
        if (transferAmountKRW == null || transferAmountKRW.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("송금 금액이 유효하지 않습니다.");

// 계좌 타입 결정
        AccountType recipientAccountType = request.getAccountType() != null ? request.getAccountType() : AccountType.KRW;
        String toCurrency = request.getCurrencyCode();

// 원화 계좌 조회
        Balance krwBalance = balanceRepository.findByUserIdAndAccountType(user.getId(), AccountType.KRW)
                .orElseThrow(() -> new RuntimeException("원화 계좌가 없습니다."));

// 공통 변수 초기화
        BigDecimal convertedAmount = transferAmountKRW;
        BigDecimal appliedRate = BigDecimal.ONE;
        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal totalDeductedAmountKRW = transferAmountKRW;

// 외화 계좌 처리
        if (recipientAccountType == AccountType.FOREIGN && request.getCurrencyCode() != null
                && supportedCurrencies.contains(request.getCurrencyCode())) {

            Balance targetForeignBalance = balanceRepository.findByUserIdAndCurrency_Code(user.getId(), request.getCurrencyCode())
                    .orElseThrow(() -> new RuntimeException(request.getCurrencyCode() + " 외화 계좌가 없습니다."));

            TransferExchangeRequest exchangeRequest = TransferExchangeRequest.builder()
                    .fromCurrency("KRW")
                    .toCurrency(request.getCurrencyCode())
                    .amount(transferAmountKRW)
                    .accountType(AccountType.FOREIGN)
                    .build();

            TransferExchangeResponse exchangeResult = foreignTransferExchangeService.simulateExchange(exchangeRequest);

            // 외환 계산 결과 반영
            appliedRate = exchangeResult.getExchangeRate();
            convertedAmount = exchangeResult.getToAmount();
            feeAmount = exchangeResult.getFee();
            totalDeductedAmountKRW = exchangeResult.getTotalDeductedAmountKRW();

            // 원화 잔액 체크 및 차감
            if (krwBalance.getAvailableAmount().compareTo(totalDeductedAmountKRW) < 0)
                throw new RuntimeException("원화 잔액 부족");

            krwBalance.setAvailableAmount(krwBalance.getAvailableAmount().subtract(totalDeductedAmountKRW));
            krwBalance.setHeldAmount(krwBalance.getHeldAmount().add(totalDeductedAmountKRW));
            balanceRepository.save(krwBalance);

            // 외화 계좌에 송금액 추가
            targetForeignBalance.setAvailableAmount(targetForeignBalance.getAvailableAmount().add(convertedAmount));
            balanceRepository.save(targetForeignBalance);

        } else {
            // KRW 계좌 처리
            TransferExchangeRequest exchangeRequest = TransferExchangeRequest.builder()
                    .fromCurrency("KRW")
                    .toCurrency("KRW")
                    .amount(transferAmountKRW)
                    .accountType(AccountType.KRW)
                    .build();

            TransferExchangeResponse exchangeResult = foreignTransferExchangeService.simulateExchange(exchangeRequest);

            convertedAmount = exchangeResult.getToAmount();
            feeAmount = exchangeResult.getFee();
            totalDeductedAmountKRW = exchangeResult.getTotalDeductedAmountKRW();

            if (krwBalance.getAvailableAmount().compareTo(totalDeductedAmountKRW) < 0)
                throw new RuntimeException("원화 잔액 부족");

            krwBalance.setAvailableAmount(krwBalance.getAvailableAmount().subtract(totalDeductedAmountKRW));
            krwBalance.setHeldAmount(krwBalance.getHeldAmount().add(totalDeductedAmountKRW));
            balanceRepository.save(krwBalance);
        }

        Transaction generalTransaction = Transaction.builder()
                .fromUser(user)
                .toUser(user)
                .transactionType(TransactionType.TRANSFER)
                .sendAmount(recipientAccountType == AccountType.FOREIGN ? convertedAmount : transferAmountKRW) // 실제 송금 통화
                .receiveAmount(recipientAccountType == AccountType.FOREIGN ? convertedAmount : transferAmountKRW)
                .exchangeRateApplied(appliedRate)
                .feeAmount(feeAmount)
                .totalDeductedAmount(totalDeductedAmountKRW)
                .fromCurrencyCode(currencyRepository.findByCode("KRW").orElseThrow())
                .toCurrencyCode(recipientAccountType == AccountType.FOREIGN
                        ? currencyRepository.findByCode(toCurrency).orElseThrow()
                        : currencyRepository.findByCode("KRW").orElseThrow())
                .status("PENDING")
                .build();
        globalTransactionRepository.save(generalTransaction);

        ForeignTransferTransaction ftTransaction = ForeignTransferTransaction.builder()
                .user(user)
                .transaction(generalTransaction)
                .requestStatus(RequestStatus.SUBMITTED)
                .transferStatus(TransferStatus.NOT_STARTED)
                .createdAt(LocalDateTime.now())
                .transferAmount(transferAmountKRW)
                .convertedAmount(convertedAmount)
                .exchangeRate(appliedRate)
                .accountPassword(request.getAccountPassword())
                .krwNumber(recipientAccountType == AccountType.KRW ? request.getAccountNumber() : null) // KRW
                .foreignNumber(recipientAccountType == AccountType.FOREIGN ? request.getAccountNumber() : null) // FOREIGN
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
        snapshot.setCurrencyCode(request.getCurrencyCode());
        snapshot.setPhoneNumber(request.getPhoneNumber());
        snapshot.setEngAddress(request.getEngAddress());
        snapshot.setCountry(request.getCountry());

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
                .accountType(recipientAccountType.name()) // KRW 또는 FOREIGN
                .accountNumber(request.getAccountNumber()) // Builder 그대로 사용
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

        return ForeignTransferResponse.builder()
                .transferId(ftTransaction.getId())
                .senderId(sender.getId())
                .accountType(recipientAccountType.name())
                .transferAmount(transferAmountKRW)
                .convertedAmount(convertedAmount)
                .appliedRate(appliedRate)
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
