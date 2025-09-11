package com.project.korex.externalAccount.dto.response;

import com.project.korex.transaction.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositWithdrawHistoryDto {
    private Long transactionId;
    private TransactionType transactionType;
    private String fromAccount;  // 외부계좌번호 또는 "원화 계좌"
    private String toAccount;    // "원화 계좌" 또는 외부계좌번호
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal balanceBefore;  // 거래 전 잔액
    private BigDecimal balanceAfter;   // 거래 후 잔액
    private LocalDateTime createdAt;
}
