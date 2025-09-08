package com.project.korex.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateJobParam {
    private String currencyCode;
    private int page;
    private int totalPages;
}
