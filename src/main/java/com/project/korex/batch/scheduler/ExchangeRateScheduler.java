package com.project.korex.batch.scheduler;

import com.project.korex.batch.service.ExchangeRateBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class ExchangeRateScheduler {

    private final ExchangeRateBatchService batchService;

    @Scheduled(cron = "0 0 9 * * MON-FRI") // 평일 오전 9시
    public void scheduledExchangeRateBatch() {
        log.info("스케줄된 환율 데이터 배치 작업 시작");
        batchService.runExchangeRateBatch();
    }
}
