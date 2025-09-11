package com.project.korex.transaction.service;

import com.project.korex.auth.service.AuthService;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.InsufficientBalanceException;
import com.project.korex.transaction.dto.response.BalanceResponseDto;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.AccountType;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final CurrencyRepository currencyRepository;
    private final UserJpaRepository userRepository;

    @Transactional(readOnly = true)
    public List<BalanceResponseDto> getUserBalances(Long userId) {
        List<Balance> balances = balanceRepository.findByUserId(userId);

        return balances.stream()
                .map(balance -> BalanceResponseDto.from(balance, balance.getCurrency()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasEnoughBalance(Long userId, String currencyCode, BigDecimal amount) {
        // Currency 객체로 조회
        Currency currency = currencyRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

        Optional<Balance> balance = balanceRepository.findByUserIdAndCurrencyForUpdate(userId, currency);
        return balance.map(b -> b.getAvailableAmount().compareTo(amount) >= 0).orElse(false);
    }

    public void deductBalance(Long userId, String currencyCode, BigDecimal amount) {
        Currency currency = currencyRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

        Balance balance = balanceRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE));

        if (balance.getAvailableAmount().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        balance.setAvailableAmount(balance.getAvailableAmount().subtract(amount));
//        balance.setHeldAmount(balance.getHeldAmount().subtract(amount));
        balanceRepository.save(balance);
    }

    public void addBalance(Long userId, String currencyCode, BigDecimal amount) {
        Currency currency = currencyRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

        Optional<Balance> existingBalance = balanceRepository.findByUserIdAndCurrencyForUpdate(userId, currency);

        if (existingBalance.isPresent()) {
            Balance balance = existingBalance.get();
            balance.setAvailableAmount(balance.getAvailableAmount().add(amount));
//            balance.setHeldAmount(balance.getHeldAmount().add(amount));
            balanceRepository.save(balance);
        } else {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            // 새 잔액 생성
            Balance newBalance = new Balance();
            newBalance.setUser(user);
            newBalance.setCurrency(currency);
            newBalance.setAvailableAmount(amount);
//            newBalance.setHeldAmount(amount);

            // 통화코드에 따라 AccountType 결정
            if ("KRW".equals(currencyCode)) {
                newBalance.setAccountType(AccountType.KRW);
            } else {
                newBalance.setAccountType(AccountType.FOREIGN);
            }

            balanceRepository.save(newBalance);
        }
    }

    /**
     * 환전용 - 모든 기본 통화 포함 잔액 조회
     */
    @Transactional(readOnly = true)
    public Map<String, BalanceResponseDto> getUserBalancesForExchange(Long userId) {
        log.info("환전용 잔액 조회 - userId: {}", userId);

        List<Balance> balances = balanceRepository.findByUserId(userId);

        // 기본 환전 통화 목록
        String[] exchangeCurrencies = {"KRW", "USD", "JPY", "EUR", "CNY", "GBP","AUD", "CAD", "CHF"};

        // 기존 잔액을 Map으로 변환 (code를 키로 사용)
        Map<String, BalanceResponseDto> balanceMap = balances.stream()
                .filter(balance -> balance.getCurrency() != null)
                .collect(Collectors.toMap(
                        balance -> balance.getCurrency().getCode(), // code 필드 사용
                        balance -> BalanceResponseDto.from(balance, balance.getCurrency()),
                        (existing, replacement) -> existing // 중복 키가 있으면 기존 값 유지
                ));

        // 없는 통화는 0으로 초기화
        for (String currencyCode : exchangeCurrencies) {
            if (!balanceMap.containsKey(currencyCode)) {
                Optional<Currency> currency = currencyRepository.findById(currencyCode);
                if (currency.isPresent()) {
                    // 0 잔액으로 Balance 생성
                    Balance zeroBalance = new Balance();
                    zeroBalance.setCurrency(currency.get());
                    zeroBalance.setAvailableAmount(BigDecimal.ZERO);

                    balanceMap.put(currencyCode, BalanceResponseDto.from(zeroBalance, currency.get()));
                } else {
                    log.warn("통화 정보를 찾을 수 없습니다: {}", currencyCode);
                }
            }
        }

        return balanceMap;
    }
}

