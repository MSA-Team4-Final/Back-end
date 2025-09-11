package com.project.korex.externalAccount.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositWithdrawSummaryDto {

    private BigDecimal totalDeposit;
    private BigDecimal totalWithdraw;
    private BigDecimal netAmount;
}
