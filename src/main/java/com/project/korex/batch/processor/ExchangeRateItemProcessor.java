package com.project.korex.batch.processor;

import com.project.korex.exchangeRate.dto.ExchangeRateDto;
import com.project.korex.exchangeRate.entity.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class ExchangeRateItemProcessor implements ItemProcessor<Map<String, String>, ExchangeRate> {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public ExchangeRate process(Map<String, String> item) throws Exception {
        try {
            ExchangeRateDto dto = mapToDto(item);
            return dtoToEntity(dto);
        } catch (Exception e) {
            log.error("데이터 변환 오류: {}", item, e);
            return null;
        }
    }

    private ExchangeRateDto mapToDto(Map<String, String> map) {
        ExchangeRateDto dto = new ExchangeRateDto();

        String baseDateStr = map.get("base_date");
        LocalDate baseDate = LocalDate.parse(baseDateStr, dateFormatter);
        dto.setBaseDate(baseDate);
        dto.setCurrencyCode(map.get("currency_code"));

        dto.setBaseRate(parseBigDecimal(map.get("base_rate")));
        dto.setChangeDirection(map.get("change_direction"));
        dto.setChangeAmount(parseBigDecimal(map.get("change_amount")));
        dto.setBuyCashRate(parseBigDecimal(map.get("buy_cash_rate")));
        dto.setSellCashRate(parseBigDecimal(map.get("sell_cash_rate")));
        dto.setSendRate(parseBigDecimal(map.get("send_rate")));
        dto.setReceiveRate(parseBigDecimal(map.get("receive_rate")));

        return dto;
    }

    private ExchangeRate dtoToEntity(ExchangeRateDto dto) {
        return ExchangeRate.builder()
                .baseDate(dto.getBaseDate())
                .currencyCode(dto.getCurrencyCode())
                .baseRate(dto.getBaseRate())
                .changeDirection(dto.getChangeDirection())
                .changeAmount(dto.getChangeAmount())
                .buyCashRate(dto.getBuyCashRate())
                .sellCashRate(dto.getSellCashRate())
                .sendRate(dto.getSendRate())
                .receiveRate(dto.getReceiveRate())
                .build();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) {
            return null;
        }
        try {
            String sanitized = value.replaceAll(",", "");
            return new BigDecimal(sanitized);
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return null;
        }
    }
}
