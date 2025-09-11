package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.response.TransferExchangeResponse;
import com.project.korex.ForeignTransfer.entity.TransferFeeAdmin;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
import com.project.korex.ForeignTransfer.service.TransferFeeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForeignTransferExchangePreviewService {

    private final TransferFeeAdminService feeAdminService;
    private final ExchangeRateCrawlerService exchangeRateCrawlerService;

    /**
     * 트랜잭션 생성 전 프론트 미리보기용 환전 계산
     *
     * @param fromCurrency 보내는 통화 (KRW, USD, JPY 등)
     * @param toCurrency 받는 통화
     * @param amount 송금 금액
     * @return 환전 계산 결과
     */
    public TransferExchangeResponse simulatePreview(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("송금 금액이 유효하지 않습니다.");
        }

        // 1️⃣ 환율 계산
        BigDecimal exchangeRate = fromCurrency.equals(toCurrency) ? BigDecimal.ONE : getExchangeRate(toCurrency);
        BigDecimal convertedAmount = calculateConvertedAmount(fromCurrency, toCurrency, amount, exchangeRate);

        // 2️⃣ 수수료 계산 (원화 기준)
        TransferFeeAdmin feePolicy = feeAdminService.getPolicyByCurrency(toCurrency)
                .orElseThrow(() -> new IllegalArgumentException(toCurrency + "에 대한 수수료 정책이 존재하지 않습니다."));

        BigDecimal feeInKRW = calculateFee(fromCurrency, amount, exchangeRate, feePolicy);

        // 3️⃣ 총 차감액 계산
        BigDecimal totalDeductedAmount = fromCurrency.equals("KRW") ? amount.add(feeInKRW) : amount;
        BigDecimal totalDeductedAmountKRW = feeInKRW;

        // JPY 소수점 처리
        if ("JPY".equals(toCurrency)) {
            convertedAmount = convertedAmount.setScale(0, RoundingMode.HALF_UP);
        } else {
            convertedAmount = convertedAmount.setScale(2, RoundingMode.HALF_UP);
        }

        return TransferExchangeResponse.builder()
                .fromAmount(amount)
                .toAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .fee(feeInKRW)
                .totalDeductedAmount(totalDeductedAmount)
                .totalDeductedAmountKRW(totalDeductedAmountKRW)
                .rateUpdateTime(LocalDateTime.now())
                .build();
    }

    private BigDecimal calculateConvertedAmount(String fromCurrency, String toCurrency, BigDecimal amount, BigDecimal exchangeRate) {
        if ("KRW".equals(fromCurrency) && !"KRW".equals(toCurrency)) {
            if ("JPY".equals(toCurrency)) {
                return amount.multiply(BigDecimal.valueOf(100)).divide(exchangeRate, 0, RoundingMode.HALF_UP);
            } else {
                return amount.divide(exchangeRate, 4, RoundingMode.HALF_UP);
            }
        } else if (!"KRW".equals(fromCurrency) && "KRW".equals(toCurrency)) {
            if ("JPY".equals(fromCurrency)) {
                return amount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        .multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            } else {
                return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            }
        } else if (fromCurrency.equals(toCurrency)) {
            return amount;
        } else {
            // 외화 간 변환: KRW 기준 환율 적용
            BigDecimal fromToKRW = ("JPY".equals(fromCurrency)) ?
                    amount.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP).multiply(getExchangeRate(fromCurrency))
                    : amount.multiply(getExchangeRate(fromCurrency));
            return fromToKRW.divide(getExchangeRate(toCurrency), 4, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculateFee(String fromCurrency, BigDecimal amount, BigDecimal exchangeRate, TransferFeeAdmin feePolicy) {
        // 원화 기준 금액으로 변환
        BigDecimal baseAmount;
        if ("KRW".equals(fromCurrency)) {
            baseAmount = amount;
        } else if ("JPY".equals(fromCurrency)) {
            baseAmount = amount.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP).multiply(getExchangeRate("JPY"));
        } else {
            baseAmount = amount.multiply(getExchangeRate(fromCurrency));
        }

        BigDecimal fee = baseAmount.multiply(BigDecimal.valueOf(feePolicy.getRate())).setScale(0, RoundingMode.HALF_UP);
        return fee.max(BigDecimal.valueOf(feePolicy.getMinFee()));
    }

    private BigDecimal getExchangeRate(String currencyCode) {
        List<Map<String, String>> cachedRates = exchangeRateCrawlerService.getRealtimeCurrencyRateFromCache(currencyCode);

        if (cachedRates == null || cachedRates.isEmpty()) {
            throw new IllegalArgumentException(currencyCode + " 환율 정보를 가져올 수 없습니다.");
        }

        String baseRateStr = cachedRates.get(0).get("send_rate");
        if (baseRateStr == null || baseRateStr.isEmpty()) {
            throw new IllegalArgumentException(currencyCode + " 환율 데이터가 비어있습니다.");
        }

        return new BigDecimal(baseRateStr.replace(",", ""));
    }
}
