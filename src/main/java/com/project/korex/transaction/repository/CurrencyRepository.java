package com.project.korex.transaction.repository;

import com.project.korex.transaction.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    List<Currency> findAllByOrderByCode();
    Optional<Currency> findByCurrencyName(String currencyName);
    Optional<Currency> findByCode(String code);

}

