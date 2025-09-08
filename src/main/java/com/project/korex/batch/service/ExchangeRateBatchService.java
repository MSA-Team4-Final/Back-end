package com.project.korex.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateBatchService {

    private final JobLauncher jobLauncher;
    private final Job exchangeRateJob;

    public void runExchangeRateBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // JobLauncher → Job → Step → Reader/Processor/Writer 순으로 자동 호출
            var jobExecution = jobLauncher.run(exchangeRateJob, jobParameters);

            log.info("배치 작업 시작: {}", jobExecution.getJobInstance().getJobName());

        } catch (Exception e) {
            log.error("배치 작업 실행 중 오류 발생", e);
            throw new RuntimeException("배치 작업 실행 실패", e);
        }
    }
}
