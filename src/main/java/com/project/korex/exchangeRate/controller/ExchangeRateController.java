package com.project.korex.exchangeRate.controller;

import com.project.korex.exchangeRate.dto.ExchangeRateDto;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
import com.project.korex.exchangeRate.service.ExchangeRateService;
import com.project.korex.exchangeRate.service.NaverNewsApiService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateCrawlerService exchangeRateCrawlerService;
    private final NaverNewsApiService naverNewsApiService;

    /**
     * 실시간 환율 데이터 조회
     * 크롤링 이후 redis 캐시에 저장한 값 보내줌
     */
    @GetMapping("/real-time")
    @Operation(summary = "실시간 환율 데이터 조회(redis데이터 or 네이버 환율 크롤링)")
    public ResponseEntity<List<Map<String, String>>> getExchangeRates() {

        // 캐시에 값이 있으면 우선 반환
        List<Map<String, String>> cached = exchangeRateCrawlerService.getRealtimeRateFromCache();
        if (cached != null && !cached.isEmpty()) {
            System.out.println("jjhdebug: from cache");
            return ResponseEntity.ok(cached);
        }
        // 없으면 크롤링 후 캐시에 저장
        try {
            List<Map<String, String>> exchangeRates = exchangeRateCrawlerService.crawlRealtimeRate();
            System.out.println("jjhdebug: from crawling");
            return ResponseEntity.ok(exchangeRates);
        } catch (IOException e) {
            // log.error("환율 데이터 크롤링 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 날짜에 여러 통화코드 환율 조회
     */
    @GetMapping("/by-date")
    @Operation(summary = "특정 날짜에 여러 통화코드 환율 조회(DataBase)")
    public List<ExchangeRateDto> getRatesByDateAndCurrencies(
//            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            @RequestParam("date") String dateStr) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        // 출력 통화 지정
        List<String> currencies = Arrays.asList("USD", "EUR", "JPY", "AUD",
                "GBP", "CNY", "CAD", "CHF");

        return exchangeRateService.getExchangeRatesByDateAndCurrencies(date, currencies);
    }

    /**
     * 특정 통화코드에 대한 일자별 환율 조회
     */
    @GetMapping("/by-currency/{currencyCode}")
    @Operation(summary = "특정 통화코드에 대한 일자별 환율 조회(DataBase)")
    public List<ExchangeRateDto> getRatesByCurrencyOrderedByDate(
            @PathVariable String currencyCode) {

        return exchangeRateService.getExchangeRatesByCurrencyOrderedByDate(currencyCode);
    }

    /**
     * Redis: 특정 통화코드의 최신 N개(기본 50개) 조회
     */
    @GetMapping("/realtime/{currencyCode}")
    @Operation(summary = "Redis에서 특정 통화의 최근 실시간 데이터 조회")
    public ResponseEntity<List<Map<String, String>>> getRealtimeByCurrency(
            @PathVariable String currencyCode
    ) {
        // 서비스에 통화별 캐시 조회 메서드가 있어야 함: getRealtimeCurrencyRateFromCache(currencyCode)
        List<Map<String, String>> list = exchangeRateCrawlerService.getRealtimeCurrencyRateFromCache(currencyCode);
        return ResponseEntity.ok(list == null ? Collections.emptyList() : list);
    }


    @GetMapping("/main-page-data")
    @Operation(summary = "메인페이지에 나타낼 최근 실시간 데이터 조회")
    public ResponseEntity<Map<String, Map<String, String>>> getMainPageData() {
        // 메인페이지에 표시할 모든 통화들
        String[] mainCurrencies = {
                "USD", "JPY", "EUR", "GBP", "CHF", "CAD",
                "AUD", "NZD", "HKD", "CNY", "SEK", "DKK",
                "NOK", "THB", "SGD"
        };

        Map<String, Map<String, String>> ratesData =
                exchangeRateCrawlerService.getMainPageRatesData(mainCurrencies);

        return ResponseEntity.ok(ratesData);
    }

    /**
     * 환율 뉴스 조회 API
     */
    @GetMapping("/news")
    public ResponseEntity<List<Map<String, String>>> getExchangeNews() throws IOException {
        List<Map<String, String>> news = naverNewsApiService.getLatestExchangeNewsFromApi();
        return ResponseEntity.ok(news);
    }


}
