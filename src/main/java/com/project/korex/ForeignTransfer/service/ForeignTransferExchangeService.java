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

        String baseRateStr = cachedRates.get(0).get("base_rate");
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
        String toCurrency = request.getToCurrency();
        boolean isForeignAccount = request.getAccountType() != null
                && request.getAccountType().name().equals("FOREIGN");

        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = fromAmount;

        if (isForeignAccount && toCurrency != null && !toCurrency.isEmpty()) {
            exchangeRate = getExchangeRate(toCurrency); // 원화 → 외화 환율
            convertedAmount = fromAmount.divide(exchangeRate, 2, RoundingMode.HALF_UP);
            if ("JPY".equals(toCurrency)) convertedAmount = convertedAmount.setScale(0, RoundingMode.HALF_UP);
        } else {
            convertedAmount = fromAmount.setScale(2, RoundingMode.HALF_UP);
        }

        // ✅ 수수료 정책은 항상 원화 기준
        TransferFeeAdmin feePolicy = getFeePolicy("KRW");
        BigDecimal feePercentage = BigDecimal.valueOf(feePolicy.getRate());
        BigDecimal minFee = BigDecimal.valueOf(feePolicy.getMinFee());
        BigDecimal feeInKRW;
        if (isForeignAccount) {
            // 외화 계좌는 원화로 환산한 금액 기준으로 수수료 산정
            feeInKRW = fromAmount.multiply(feePercentage).setScale(0, RoundingMode.HALF_UP);
        } else {
            // 원화 계좌는 그대로 원화 금액 기준으로 산정
            feeInKRW = fromAmount.multiply(feePercentage).setScale(0, RoundingMode.HALF_UP);
        }
        if (feeInKRW.compareTo(minFee) < 0) feeInKRW = minFee;

        BigDecimal totalDeductedAmount = fromAmount.add(feeInKRW);
        BigDecimal totalDeductedAmountKRW = totalDeductedAmount;

        // 일본 JPY는 소수점 제거
        if (isForeignAccount && "JPY".equals(toCurrency)) {
            convertedAmount = convertedAmount.setScale(0, RoundingMode.HALF_UP);
        } else {
            convertedAmount = convertedAmount.setScale(2, RoundingMode.HALF_UP);
        }

        return TransferExchangeResponse.builder()
                .fromAmount(fromAmount)
                .toAmount(convertedAmount)
                .exchangeRate(exchangeRate) // ✅ null 절대 아님
                .fee(feeInKRW)
                .totalDeductedAmount(totalDeductedAmount)
                .totalDeductedAmountKRW(totalDeductedAmountKRW)
                .rateUpdateTime(LocalDateTime.now())
                .build();
    }
}
