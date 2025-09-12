package com.project.korex.ForeignTransfer.entity;

import com.project.korex.common.BaseEntity;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ForeignTransferRecipient")
@Getter
@Setter
public class Recipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipient_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(name = "name")
    private String name;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "country_number")
    private String countryNumber;

    @Column(name = "country")
    private String country;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "eng_address")
    private String engAddress;

    @Column(name = "is_active")
    private Boolean isActive = true;  // 기본값 true

}