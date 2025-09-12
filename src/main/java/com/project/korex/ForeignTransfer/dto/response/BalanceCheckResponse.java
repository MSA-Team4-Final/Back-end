// BalanceCheckResponse.java
package com.project.korex.ForeignTransfer.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BalanceCheckResponse {
    private List<CurrencyBalance> balances = new ArrayList<>();

    @Data
    public static class CurrencyBalance {  // static으로 수정
        private String accountType;
        private String currencyCode;
        private BigDecimal availableAmount;
        private BigDecimal heldAmount;
        private BigDecimal totalAmount;
        private String accountNumber;
    }
}
