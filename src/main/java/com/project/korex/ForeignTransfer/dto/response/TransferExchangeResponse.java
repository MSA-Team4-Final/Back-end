package com.project.korex.ForeignTransfer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransferExchangeResponse {
    private BigDecimal fromAmount;             // 송금 금액
    private BigDecimal toAmount;               // 환전 금액
    private BigDecimal exchangeRate;           // 외화 계좌일 때만 표시
    private BigDecimal fee;                    // 원화 기준 수수료
    private BigDecimal totalDeductedAmount;    // 계좌 기준 총 차감액 (KRW / 외화 계좌)
    private BigDecimal totalDeductedAmountKRW; // 원화 기준 총 차감액 (외화 계좌 참고용)
    private LocalDateTime rateUpdateTime;
    private BigDecimal frontTotalDeductedAmount;
}
