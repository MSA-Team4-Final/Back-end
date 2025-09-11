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

    public TransferFeeAdmin getFeePolicy(String currencyCode) {
        Optional<TransferFeeAdmin> policyOpt = feeAdminService.getPolicyByCurrency(currencyCode);
        return policyOpt.orElseThrow(() ->
                new IllegalArgumentException(currencyCode + "에 대한 수수료 정책이 존재하지 않습니다."));
    }

    /**
     * 실제 외부 환율 조회 (예외 발생 가능).
     */
    public BigDecimal getExchangeRate(String currencyCode) {
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

    /**
     * 안전한 환율 조회 : KRW이면 1, 그 외에는 getExchangeRate 호출.
     * (KRW에 대해 외부 API가 없는 경우 예외를 방지)
     */
    public BigDecimal getExchangeRateSafe(String currencyCode) {
        if ("KRW".equalsIgnoreCase(currencyCode)) return BigDecimal.ONE;
        return getExchangeRate(currencyCode);
    }

    /**
     * 환전 및 수수료 계산 응답 생성
     * fromCurrency/toCurrency : ISO 코드 (KRW, USD, JPY ...)
     * amount : 송금 계좌에서 차감되는 '보내는 금액' (fromCurrency 기준)
     */
    public TransferExchangeResponse executeExchange(TransferExchangeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("송금 금액이 유효하지 않습니다.");
        }

        BigDecimal fromAmount = request.getAmount();
        String fromCurrency = request.getFromCurrency();
        String toCurrency = request.getToCurrency();

        // 기본값(동일통화)
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = fromAmount;

        // === 1) 환율 & 변환 계산 ===
        if ("KRW".equalsIgnoreCase(fromCurrency) && !"KRW".equalsIgnoreCase(toCurrency)) {
            // KRW -> 외화 : 외화의 KRW 환율(예: USD 환율)을 사용하여 외화금액 계산
            exchangeRate = getExchangeRateSafe(toCurrency);
            if ("JPY".equalsIgnoreCase(toCurrency)) {
                // JPY는 100 단위 규칙
                convertedAmount = fromAmount.multiply(BigDecimal.valueOf(100)).divide(exchangeRate, 0, RoundingMode.HALF_UP);
            } else {
                convertedAmount = fromAmount.divide(exchangeRate, 4, RoundingMode.HALF_UP);
            }
        } else if (!"KRW".equalsIgnoreCase(fromCurrency) && "KRW".equalsIgnoreCase(toCurrency)) {
            // 외화 -> KRW : 외화의 KRW 환율로 KRW 계산
            exchangeRate = getExchangeRateSafe(fromCurrency);
            if ("JPY".equalsIgnoreCase(fromCurrency)) {
                convertedAmount = fromAmount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        .multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            } else {
                convertedAmount = fromAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            }
        } else if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            // 동일 통화 (ex: USD -> USD, KRW -> KRW)
            exchangeRate = BigDecimal.ONE;
            convertedAmount = fromAmount;
        } else {
            // 외화 간 환전(예: USD -> EUR) 필요하면 여기에 로직 추가.
            // 현재 시스템은 'KRW <-> 외화' 와 '동일 외화' 만 지원한다고 가정.
            throw new IllegalArgumentException("지원하지 않는 통화 변환입니다: " + fromCurrency + " → " + toCurrency);
        }

        // JPY 소수점 처리(수취 통화 기준)
        if ("JPY".equalsIgnoreCase(toCurrency)) {
            convertedAmount = convertedAmount.setScale(0, RoundingMode.HALF_UP);
        } else {
            convertedAmount = convertedAmount.setScale(2, RoundingMode.HALF_UP);
        }

        // === 2) 수수료 계산 (항상 KRW 기준) ===
        TransferFeeAdmin feePolicy = getFeePolicy(toCurrency); // 정책은 수취통화 기준으로 조회(요구사항에 맞도록)
        BigDecimal feePercentage = BigDecimal.valueOf(feePolicy.getRate());
        BigDecimal minFee = BigDecimal.valueOf(feePolicy.getMinFee());

        // baseAmountForFee: 송금액을 KRW로 환산한 값 (수수료는 KRW 기준)
        BigDecimal baseAmountForFee;
        if ("KRW".equalsIgnoreCase(fromCurrency)) {
            baseAmountForFee = fromAmount;
        } else if ("JPY".equalsIgnoreCase(fromCurrency)) {
            // JPY는 100 단위로 나눠서 KRW 환율 적용
            baseAmountForFee = fromAmount.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                    .multiply(getExchangeRateSafe("JPY"));
        } else {
            // 기타 외화 -> KRW 적용 (환율 안전 조회)
            baseAmountForFee = fromAmount.multiply(getExchangeRateSafe(fromCurrency));
        }

        BigDecimal feeInKRW = baseAmountForFee.multiply(feePercentage).setScale(0, RoundingMode.HALF_UP);
        if (feeInKRW.compareTo(minFee) < 0) feeInKRW = minFee;

        // 외화 계좌에서 차감해야 하는 경우 외화 수수료 변환(필요시)
        BigDecimal feeInForeign = null;
        if (!"KRW".equalsIgnoreCase(fromCurrency)) {
            // 외화로 수수료를 자동으로 계산해 둘 수도 있음(현재는 원화로 차감하므로 null 허용)
            // 필요하면 feeInForeign = feeInKRW.divide(getExchangeRateSafe(fromCurrency), 4, RoundingMode.HALF_UP);
        }

        // === 3) 총 차감액(송금계좌에서 빠지는 금액) 및 총 KRW 차감(수수료 포함) ===
        BigDecimal totalDeductedAmount;
        BigDecimal totalDeductedAmountKRW;

        if ("KRW".equalsIgnoreCase(fromCurrency)) {
            // 원화 송금 : 송금금액 + 수수료(모두 KRW에서 차감)
            totalDeductedAmount = fromAmount.add(feeInKRW);
            totalDeductedAmountKRW = totalDeductedAmount;
        } else {
            // 외화 송금 : 송금금액은 외화에서만 차감, 수수료는 원화계좌에서 별도 차감
            totalDeductedAmount = fromAmount;
            totalDeductedAmountKRW = feeInKRW;
        }

        return TransferExchangeResponse.builder()
                .fromAmount(fromAmount)
                .toAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .fee(feeInKRW)
                .feeInForeign(feeInForeign)
                .totalDeductedAmount(totalDeductedAmount)
                .totalDeductedAmountKRW(totalDeductedAmountKRW)
                .rateUpdateTime(LocalDateTime.now())
                .build();
    }
}
