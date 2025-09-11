package com.project.korex.transaction.repository;

import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    List<Balance> findByUserId(Long userId);

    Optional<Balance> findByUserIdAndCurrency(Long userId, Currency currency);

    Optional<Balance> findByUserIdAndCurrencyCode(Long userId, String currencyCode);

    Optional<Balance> findByUserIdAndAccountType(Long userId, AccountType accountType);

    Optional<Balance> findByUserIdAndCurrency_Code(Long userId, String currencyCode);
}

