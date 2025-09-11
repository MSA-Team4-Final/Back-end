package com.project.korex.ForeignTransfer.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PreviewExchangeRequest {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal amount;
}
