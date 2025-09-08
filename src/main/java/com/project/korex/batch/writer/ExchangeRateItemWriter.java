package com.project.korex.batch.writer;

import com.project.korex.exchangeRate.entity.ExchangeRate;
import com.project.korex.exchangeRate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateItemWriter implements ItemWriter<ExchangeRate> {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends ExchangeRate> chunk) throws Exception {
        List<? extends ExchangeRate> items = chunk.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        // ★ 기준일 + 통화코드별로 그룹화
        Map<String, List<ExchangeRate>> groupedByDateAndCurrency = items.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getBaseDate() + "_" + item.getCurrencyCode()));

        for (Map.Entry<String, List<ExchangeRate>> entry : groupedByDateAndCurrency.entrySet()) {
            List<ExchangeRate> dateAndCurrencyItems = entry.getValue();

            if (!dateAndCurrencyItems.isEmpty()) {
                LocalDate baseDate = dateAndCurrencyItems.get(0).getBaseDate();
                String currencyCode = dateAndCurrencyItems.get(0).getCurrencyCode();

                // ★ 특정 날짜 + 특정 통화만 삭제
                exchangeRateRepository.deleteByBaseDateAndCurrencyCode(baseDate, currencyCode);
                exchangeRateRepository.saveAll(dateAndCurrencyItems);

                log.info("환율 데이터 저장 완료: {} 건, 기준일: {}, 통화: {}",
                        dateAndCurrencyItems.size(), baseDate, currencyCode);
            }
        }
    }
}
