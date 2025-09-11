package com.project.korex.externalAccount.service;

import com.project.korex.externalAccount.dto.request.DepositRequestDto;
import com.project.korex.externalAccount.dto.request.WithdrawRequestDto;
import com.project.korex.externalAccount.entity.ExternalAccount;
import com.project.korex.externalAccount.entity.TransactionExternalAccount;
import com.project.korex.externalAccount.enums.AccountRole;
import com.project.korex.externalAccount.repository.ExternalAccountRepository;
import com.project.korex.externalAccount.repository.TransactionExternalAccountRepository;
import com.project.korex.transaction.dto.response.TransactionResponseDto;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.transaction.service.BalanceService;
import com.project.korex.user.entity.Users;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DepositWithdrawService {

    private final TransactionRepository transactionRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final CurrencyRepository currencyRepository; // Currency 조회용 추가
    private final TransactionExternalAccountRepository transactionExternalAccountRepository;
    private final BalanceService balanceService;

    private final PasswordEncoder passwordEncoder;

    private static final BigDecimal WITHDRAWAL_FEE = new BigDecimal("1000");
    private static final String KRW_CURRENCY = "KRW";

    public TransactionResponseDto deposit(Users user, DepositRequestDto request) {
        // 거래 비밀번호 검증

//        if(!user.getTransactionPassword().equals(request.getTransactionPassword())){
//            throw new IllegalArgumentException("거래 비밀번호가 일치하지 않습니다.");
//        }
        if (!passwordEncoder.matches(request.getTransactionPassword(),
                user.getTransactionPassword())) {
            throw new IllegalArgumentException("거래 비밀번호가 일치하지 않습니다");
        }

        // 주계좌 조회
        ExternalAccount primaryAccount = externalAccountRepository
                .findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new EntityNotFoundException("주계좌가 등록되지 않았습니다"));

        // 주계좌 잔액 검증
        if (primaryAccount.getSimulationBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("주계좌 잔액이 부족합니다");
        }

        // Currency 객체 조회
        Currency krwCurrency = currencyRepository.findById(KRW_CURRENCY)
                .orElseThrow(() -> new EntityNotFoundException("KRW 통화 정보를 찾을 수 없습니다"));

        // 거래 기록 생성
        Transaction transaction = createDepositTransaction(user, request.getAmount(), krwCurrency);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // **추가: 외부계좌 매핑 정보 저장**
        saveExternalAccountMapping(savedTransaction, primaryAccount, AccountRole.FROM);
        try {
            // KRW 잔액 추가 (기존 BalanceService 활용)
            balanceService.addBalance(user.getId(), KRW_CURRENCY, request.getAmount());

            // 외부계좌 잔액 차감
            primaryAccount.setSimulationBalance(
                    primaryAccount.getSimulationBalance().subtract(request.getAmount()));
            externalAccountRepository.save(primaryAccount);

            log.info("충전 성공 - userId: {}, amount: {}", user.getId(), request.getAmount());

        } catch (Exception e) {
            log.error("충전 중 오류 발생 - userId: {}, error: {}", user.getId(), e.getMessage());
            throw new RuntimeException("충전 처리 중 오류가 발생했습니다.");
        }

        return TransactionResponseDto.builder()
                .id(savedTransaction.getId())
                .transactionType(TransactionType.DEPOSIT)
                .receiveAmount(request.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .totalDeductedAmount(request.getAmount())
                .status("COMPLETE")
//                .description("KRW 계좌 충전")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public TransactionResponseDto withdraw(Users user, WithdrawRequestDto request) {
        BigDecimal finalAmount = request.getAmount().subtract(WITHDRAWAL_FEE);

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("출금 금액이 수수료보다 작습니다");
        }

        // 거래 비밀번호 검증
//        if(!user.getTransactionPassword().equals(request.getTransactionPassword())){
//            throw new IllegalArgumentException("거래 비밀번호가 일치하지 않습니다.");
//        }
        if (!passwordEncoder.matches(request.getTransactionPassword(),
                user.getTransactionPassword())) {
            throw new IllegalArgumentException("거래 비밀번호가 일치하지 않습니다");
        }

        // KRW 잔액 확인 (기존 BalanceService 활용)
        if (!balanceService.hasEnoughBalance(user.getId(), KRW_CURRENCY, request.getAmount())) {
            throw new IllegalArgumentException("KRW 계좌 잔액이 부족합니다");
        }

        // 주계좌 조회
        ExternalAccount primaryAccount = externalAccountRepository
                .findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new EntityNotFoundException("주계좌가 등록되지 않았습니다"));

        // Currency 객체 조회
        Currency krwCurrency = currencyRepository.findById(KRW_CURRENCY)
                .orElseThrow(() -> new EntityNotFoundException("KRW 통화 정보를 찾을 수 없습니다"));

        // 거래 기록 생성
        Transaction transaction = createWithdrawTransaction(user, request.getAmount(), finalAmount, krwCurrency);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // **추가: 외부계좌 매핑 정보 저장**
        saveExternalAccountMapping(savedTransaction, primaryAccount, AccountRole.TO);

        try {
            // KRW 잔액 차감 (기존 BalanceService 활용)
            balanceService.deductBalance(user.getId(), KRW_CURRENCY, request.getAmount());

            // 외부계좌 잔액 추가 (수수료 제외한 실제 출금액)
            primaryAccount.setSimulationBalance(
                    primaryAccount.getSimulationBalance().add(finalAmount));
            externalAccountRepository.save(primaryAccount);

            log.info("출금 성공 - userId: {}, amount: {}, finalAmount: {}",
                    user.getId(), request.getAmount(), finalAmount);

        } catch (Exception e) {
            log.error("출금 중 오류 발생 - userId: {}, error: {}", user.getId(), e.getMessage());
            throw new RuntimeException("출금 처리 중 오류가 발생했습니다.");
        }

        return TransactionResponseDto.builder()
                .id(savedTransaction.getId())
                .transactionType(TransactionType.WITHDRAW)
                .receiveAmount(request.getAmount())
                .feeAmount(WITHDRAWAL_FEE)
                .totalDeductedAmount(finalAmount)
                .status("COMPLETE")
//                .description("KRW 계좌 출금")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Transaction createDepositTransaction(Users user, BigDecimal amount, Currency currency) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(user); // 외부에서 입금
        transaction.setToUser(user);
        transaction.setFromCurrencyCode(currency); // Currency 객체 설정
        transaction.setToCurrencyCode(currency);   // Currency 객체 설정
        transaction.setSendAmount(amount);
        transaction.setReceiveAmount(amount);
        transaction.setFeePercentage(BigDecimal.ZERO);
        transaction.setExchangeRateApplied(BigDecimal.ONE);
        transaction.setFeeAmount(BigDecimal.ZERO);
        transaction.setTotalDeductedAmount(amount);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus("COMPLETE");
        return transaction;
    }

    private Transaction createWithdrawTransaction(Users user, BigDecimal amount, BigDecimal finalAmount, Currency currency) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(user);
        transaction.setToUser(user); // 외부로 출금
        transaction.setFromCurrencyCode(currency); // Currency 객체 설정
        transaction.setToCurrencyCode(currency);   // Currency 객체 설정
        transaction.setSendAmount(amount);
        transaction.setReceiveAmount(finalAmount);
        transaction.setFeePercentage(BigDecimal.ZERO);
        transaction.setExchangeRateApplied(BigDecimal.ONE);
        transaction.setFeeAmount(WITHDRAWAL_FEE);
        transaction.setTotalDeductedAmount(amount);
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setStatus("COMPLETE");
        return transaction;
    }

    // **추가: 외부계좌 매핑 정보 저장 메서드**
    private void saveExternalAccountMapping(Transaction transaction, ExternalAccount externalAccount, AccountRole role) {
        try {
            TransactionExternalAccount mapping = new TransactionExternalAccount();
            mapping.setTransaction(transaction);
            mapping.setExternalAccount(externalAccount);
            mapping.setAccountRole(role);

            transactionExternalAccountRepository.save(mapping);

            log.debug("외부계좌 매핑 저장 완료 - transactionId: {}, externalAccountId: {}, role: {}",
                    transaction.getId(), externalAccount.getId(), role);

        } catch (Exception e) {
            log.error("외부계좌 매핑 저장 실패 - transactionId: {}, error: {}",
                    transaction.getId(), e.getMessage());
            // 매핑 저장 실패는 거래 자체를 실패시키지 않음 (선택사항)
        }
    }

}

