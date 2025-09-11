package com.project.korex.ForeignTransfer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ForeignTransferResponse {

    private Long transferId;
    private Long senderId;
    private String accountType;
    private BigDecimal transferAmount;       // 입력 금액
    private BigDecimal convertedAmount;      // 환전 금액
    private BigDecimal appliedRate;          // 적용 환율
    private BigDecimal feeAmount;            // 수수료
    private BigDecimal feePercentage;        // 수수료 비율
    private BigDecimal frontTotalDeductedAmount; // 총 차감액
    private String krwAccount;               // 출금 계좌
    private String foreignAccount;           // 수취인 계좌
    private String transferReason;
    private String relationRecipient;
    private String requestStatus;            // 요청 상태 (예: PENDING)
    private String transferStatus;           // 송금 상태 (예: COMPLETED)
    private LocalDateTime createdAt;

    private boolean termsAgreed;             // 약관 동의 여부
    private LocalDateTime agreedAt;          // 약관 동의 시각
}
