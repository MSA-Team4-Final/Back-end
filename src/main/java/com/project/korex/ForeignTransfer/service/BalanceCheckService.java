// BalanceCheckService.java
package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.response.BalanceCheckResponse;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.enums.AccountType;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceCheckService {

    private final UserJpaRepository userRepository;
    private final BalanceRepository balanceRepository;

    // 전체 계좌 조회
    public BalanceCheckResponse checkAllBalances(String loginId) {
        Users user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        BalanceCheckResponse response = new BalanceCheckResponse();

        // KRW 계좌
        balanceRepository.findByUserIdAndAccountType(user.getId(), AccountType.KRW)
                .ifPresent(balance -> response.getBalances().add(toCurrencyBalance(balance)));

        // 외화 계좌
        List<Balance> allBalances = balanceRepository.findByUserId(user.getId());
        allBalances.stream()
                .filter(b -> b.getAccountType() == AccountType.FOREIGN)
                .forEach(b -> response.getBalances().add(toCurrencyBalance(b)));

        return response;
    }

    // 단일 계좌 조회
    public BalanceCheckResponse checkBalance(String loginId, AccountType accountType, String currencyCode) {
        Users user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Balance balance;
        if (accountType == AccountType.KRW) {
            balance = balanceRepository.findByUserIdAndAccountType(user.getId(), AccountType.KRW)
                    .orElseThrow(() -> new RuntimeException("원화 계좌 잔액 정보가 없습니다."));
        } else {
            balance = balanceRepository.findByUserIdAndCurrency_Code(user.getId(), currencyCode)
                    .orElseThrow(() -> new RuntimeException(currencyCode + " 잔액 정보가 없습니다."));
        }

        BalanceCheckResponse response = new BalanceCheckResponse();
        response.getBalances().add(toCurrencyBalance(balance));
        return response;
    }

    private BalanceCheckResponse.CurrencyBalance toCurrencyBalance(Balance balance) {
        BalanceCheckResponse.CurrencyBalance cb = new BalanceCheckResponse.CurrencyBalance();

        cb.setAccountType(balance.getAccountType().name());
        cb.setCurrencyCode(balance.getCurrency() != null ? balance.getCurrency().getCode() : "KRW");
        cb.setAvailableAmount(balance.getAvailableAmount());
        cb.setHeldAmount(balance.getHeldAmount() != null ? balance.getHeldAmount() : BigDecimal.ZERO);
        cb.setTotalAmount(cb.getAvailableAmount().add(cb.getHeldAmount()));

        Users user = balance.getUser();
        cb.setAccountNumber(balance.getAccountType() == AccountType.KRW ? user.getKrwAccount() : user.getForeignAccount());

        return cb;
    }
}
