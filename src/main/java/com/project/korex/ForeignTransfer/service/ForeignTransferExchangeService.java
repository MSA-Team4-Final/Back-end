package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.request.TransferExchangeRequest;
import com.project.korex.ForeignTransfer.dto.response.TransferExchangeResponse;
import com.project.korex.ForeignTransfer.entity.TransferFeeAdmin;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
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
public class ForeignTransferExchangeService {

    private final TransferFeeAdminService feeAdminService;
    private final ExchangeRateCrawlerService exchangeRateCrawlerService;

    private TransferFeeAdmin getFeePolicy(String currencyCode) {
        Optional<TransferFeeAdmin> policyOpt = feeAdminService.getPolicyByCurrency(currencyCode);
        return policyOpt.orElseThrow(() ->
                new IllegalArgumentException(currencyCode + "에 대한 수수료 정책이 존재하지 않습니다."));
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

    public TransferExchangeResponse simulateExchange(TransferExchangeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("송금 금액이 유효하지 않습니다.");
        }

        BigDecimal fromAmount = request.getAmount();
        String fromCurrency = request.getFromCurrency();
        String toCurrency = request.getToCurrency();

        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = fromAmount;

        // 1️⃣ 환율 계산
        if ("KRW".equals(fromCurrency) && !"KRW".equals(toCurrency)) {
            exchangeRate = getExchangeRate(toCurrency);
            if ("JPY".equals(toCurrency)) {
                // 100엔 기준
                convertedAmount = fromAmount.multiply(BigDecimal.valueOf(100))
                        .divide(exchangeRate, 0, RoundingMode.HALF_UP);
            } else {
                convertedAmount = fromAmount.divide(exchangeRate, 2, RoundingMode.HALF_UP);
            }
        } else if (!"KRW".equals(fromCurrency) && "KRW".equals(toCurrency)) {
            exchangeRate = getExchangeRate(fromCurrency);
            if ("JPY".equals(fromCurrency)) {
                // 100엔 기준
                convertedAmount = fromAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            } else {
                convertedAmount = fromAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            }
        } else if (fromCurrency.equals(toCurrency)) {
            exchangeRate = BigDecimal.ONE;
            convertedAmount = fromAmount;
        } else {
            throw new IllegalArgumentException("지원하지 않는 통화 변환입니다: " + fromCurrency + " → " + toCurrency);
        }

        // JPY 소수점 처리
        if ("JPY".equals(toCurrency)) {
            convertedAmount = convertedAmount.setScale(0, RoundingMode.HALF_UP);
        } else {
            convertedAmount = convertedAmount.setScale(2, RoundingMode.HALF_UP);
        }

        // 2️⃣ 수수료 계산 (원화 기준)
        TransferFeeAdmin feePolicy = getFeePolicy("KRW");
        BigDecimal feePercentage = BigDecimal.valueOf(feePolicy.getRate());
        BigDecimal minFee = BigDecimal.valueOf(feePolicy.getMinFee());

        BigDecimal baseAmountForFee = "KRW".equals(fromCurrency) ? fromAmount
                : ("JPY".equals(fromCurrency)
                ? fromAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(exchangeRate)
                : fromAmount.multiply(exchangeRate));

        BigDecimal feeInKRW = baseAmountForFee.multiply(feePercentage).setScale(0, RoundingMode.HALF_UP);
        if (feeInKRW.compareTo(minFee) < 0) feeInKRW = minFee;

        // 3️⃣ 총 차감액 계산
        BigDecimal totalDeductedAmount;      // 실제 송금 계좌에서 차감되는 금액
        BigDecimal totalDeductedAmountKRW;   // 원화 계좌 기준 차감액 (수수료 포함)
        BigDecimal frontTotalDeductedAmount; // 프론트 표시용

        if ("KRW".equals(fromCurrency)) {
            // 원화 계좌: 수수료 포함
            totalDeductedAmount = fromAmount.add(feeInKRW);
            totalDeductedAmountKRW = totalDeductedAmount;
            frontTotalDeductedAmount = feeInKRW;
        } else {
            // 외화 계좌: 외화 송금액 차감 / 원화 수수료 차감
            totalDeductedAmount = fromAmount;
            totalDeductedAmountKRW = feeInKRW;
            frontTotalDeductedAmount = convertedAmount;
        }

        return TransferExchangeResponse.builder()
                .fromAmount(fromAmount)
                .toAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .fee(feeInKRW)
                .totalDeductedAmount(totalDeductedAmount)
                .totalDeductedAmountKRW(totalDeductedAmountKRW)
                .frontTotalDeductedAmount(frontTotalDeductedAmount)
                .rateUpdateTime(LocalDateTime.now())
                .build();
    }
}
