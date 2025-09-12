package com.project.korex.ForeignTransfer.dto.request;

import lombok.Data;

@Data
public class RecipientRequest {

    private String name;
    private String bankName;
    private String accountNumber;
    private String countryNumber;
    private String country;
    private String phoneNumber;
    private String email;
    private String currencyCode; // "USD"
    private String engAddress;

}