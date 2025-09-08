package com.project.korex.exchangeRate.repository;

import com.project.korex.exchangeRate.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // 특정 날짜에 해당하는 모든 환율 data 조회
    List<ExchangeRate> findByBaseDate(LocalDate baseDate);

    // 특정 통화코드의 일자 환율 데이터 조회(날짜 오름차순)
    List<ExchangeRate> findByCurrencyCodeOrderByBaseDateAsc(String currencyCode);

    // 특정 날짜 및 통화 코드 리스트에 해당하는 환율 조회
    List<ExchangeRate> findByBaseDateAndCurrencyCodeIn(LocalDate baseDate, List<String> currencyCodes);

    void deleteByBaseDate(LocalDate baseDate);

    @Modifying
    @Query("DELETE FROM ExchangeRate e WHERE e.baseDate = :baseDate AND e.currencyCode = :currencyCode")
    void deleteByBaseDateAndCurrencyCode(@Param("baseDate") LocalDate baseDate,
                                         @Param("currencyCode") String currencyCode);


}