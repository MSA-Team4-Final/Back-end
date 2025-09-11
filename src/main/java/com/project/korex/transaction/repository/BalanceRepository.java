package com.project.korex.transaction.repository;

import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.user.id = :userId AND b.currency = :currency")
    Optional<Balance> findByUserIdAndCurrencyForUpdate(@Param("userId") Long userId,
                                                       @Param("currency") Currency currency);

    Optional<Balance> findByUserIdAndCurrency(Long userId, Currency currency);

    Optional<Balance> findByUserIdAndCurrencyCode(Long userId, String currencyCode);
}

