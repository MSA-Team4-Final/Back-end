package com.project.korex.batch.reader;

import com.project.korex.batch.util.SafeCrawlingManager;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ExchangeRateItemReader extends AbstractItemCountingItemStreamItemReader<Map<String, String>> {

    private final ExchangeRateCrawlerService crawlerService;
    private final SafeCrawlingManager crawlingManager;

    @Value("${batch.currency.codes:USD,EUR}")
    private List<String> currencyCodes;

    @Value("${batch.currency.max-pages:3}")
    private int maxPages;

    private List<Map<String, String>> allData;
    private int currentIndex = 0;

    public ExchangeRateItemReader(ExchangeRateCrawlerService crawlerService,
                                  SafeCrawlingManager crawlingManager) {
        this.crawlerService = crawlerService;
        this.crawlingManager = crawlingManager;
        setName("ExchangeRateItemReader");
    }

    @Override
    protected void doOpen() throws Exception {
        allData = new ArrayList<>();
        currentIndex = 0;
        safeCrawlAllData();
    }

    private void safeCrawlAllData() {
        log.info("안전 크롤링 시작 - 총 {}개 통화, {}페이지씩", currencyCodes.size(), maxPages);

        for (int currencyIndex = 0; currencyIndex < currencyCodes.size(); currencyIndex++) {
            String currencyCode = currencyCodes.get(currencyIndex);

            if (!crawlingManager.canMakeRequest()) {
                log.warn("요청 한도 초과로 {} 통화 스킵", currencyCode);
                continue;
            }

            log.info("통화 {} 크롤링 시작", currencyCode);

            for (int page = 1; page <= maxPages; page++) {

                if (!crawlingManager.canMakeRequest()) {
                    log.warn("요청 한도 초과로 크롤링 중단");
                    return;
                }

                try {
                    // 동적 지연시간 적용 (8~20초)
                    long delay = crawlingManager.getRecommendedDelay();
                    log.info("{}ms 대기 후 크롤링: {} 페이지 {}", delay, currencyCode, page);
                    Thread.sleep(delay);

                    // 크롤링 실행
                    List<Map<String, String>> pageData = crawlerService.crawlDailyRate(currencyCode, page);
                    crawlingManager.recordRequest();

                    if (pageData != null && !pageData.isEmpty()) {
                        pageData.forEach(data -> data.put("currency_code", currencyCode));
                        allData.addAll(pageData);
                        log.info("크롤링 성공: {} 페이지 {}, {} 건", currencyCode, page, pageData.size());
                    } else {
                        log.info("데이터 없음: {} 페이지 {} - 다음 통화로", currencyCode, page);
                        break;
                    }

                } catch (IOException e) {
                    log.error("크롤링 실패: {} 페이지 {} - 30초 대기 후 다음 통화로", currencyCode, page, e);
                    try {
                        Thread.sleep(30000); // 30초 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("크롤링 인터럽트", e);
                    return;
                }
            }

            // 통화 간 추가 대기
            if (currencyIndex < currencyCodes.size() - 1) {
                try {
                    log.info("다음 통화 대기: 15초");
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        log.info("안전 크롤링 완료: {} 건", allData.size());
    }

    @Override
    protected Map<String, String> doRead() throws Exception {
        if (currentIndex < allData.size()) {
            return allData.get(currentIndex++);
        }
        return null;
    }

    @Override
    protected void doClose() throws Exception {
        if (allData != null) {
            allData.clear();
            allData = null;
        }
        currentIndex = 0;
    }
}
