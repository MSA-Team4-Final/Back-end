package com.project.korex.ForeignTransfer.dto.request;

import com.project.korex.transaction.enums.AccountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TransferExchangeRequest {
    private final String fromCurrency;
    private final String toCurrency;
    private final BigDecimal amount;
    private final BigDecimal exchangeRate;
    private AccountType accountType;
}
