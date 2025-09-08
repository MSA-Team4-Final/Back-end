package com.project.korex.ForeignTransfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "foreign_transfer_recipient_snapshot")
@Getter
@Setter
public class RecipientSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "recipient_name", nullable = false)
    private String name;

    @Column(name = "recipient_bank", nullable = false)
    private String bankName;

    @Column(name = "recipient_account_number", nullable = false)
    private String accountNumber;

    @Column(name = "recipient_email")
    private String email;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "country")
    private String country;

    @Column(name = "eng_address")
    private String engAddress;

    // 거래내역과 1:1 매핑
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private ForeignTransferTransaction transaction;
}
