package com.project.korex.ForeignTransfer.dto.response;

import lombok.Data;

@Data
public class RecipientResponse {

    private Long recipientId;
    private String name;
    private String bankName;
    private String accountNumber;
    private String countryNumber;
    private String country;
    private String phoneNumber;
    private String email;
    private String currencyCode; // "USD"
    private String currencyName; // "미국 달러
    private String engAddress;
    private Boolean isActive; // 추가

}