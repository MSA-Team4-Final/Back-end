package com.project.korex.batch.config;

import com.project.korex.exchangeRate.entity.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ExchangeRateBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job exchangeRateJob(Step exchangeRateStep) {
        return new JobBuilder("exchangeRateJob", jobRepository)
                .start(exchangeRateStep)
                .build();
    }

    @Bean
    public Step exchangeRateStep(
            ItemReader<Map<String, String>> exchangeRateReader,
            ItemProcessor<Map<String, String>, ExchangeRate> exchangeRateProcessor,
            ItemWriter<ExchangeRate> exchangeRateWriter) {

        return new StepBuilder("exchangeRateStep", jobRepository)
                .<Map<String, String>, ExchangeRate>chunk(5, transactionManager) // 청크 크기 줄임
                .reader(exchangeRateReader)
                .processor(exchangeRateProcessor)
                .writer(exchangeRateWriter)
                .faultTolerant()
                .retryLimit(2)
                .retry(IOException.class)
                .skipLimit(3)
                .skip(Exception.class)
                .build();
    }
}
