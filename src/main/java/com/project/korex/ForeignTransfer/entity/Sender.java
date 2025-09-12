package com.project.korex.ForeignTransfer.entity;

import com.project.korex.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ForeignTransferSender")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sender_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "transfer_id") // 외래 키 컬럼명
    private ForeignTransferTransaction foreignTransferTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "name")
    private String name;

    @Column(name = "transfer_reason")
    private String transferReason;

    @Column(name = "country_number")
    private String countryNumber;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "country")
    private String country;

    @Column(name = "eng_address")
    private String engAddress;

    @Column(name = "relation_recipient")
    private String relationRecipient;

    @Column(name = "id_file_path")
    private String idFilePath;

    @Column(name = "proof_document_file_path")
    private String proofDocumentFilePath;

    @Column(name = "relation_document_file_path")
    private String relationDocumentFilePath;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "account_number")
    private String accountNumber;

    // 편의 메서드: ForeignTransferTransaction과 양방향 설정
    public void setForeignTransferTransaction(ForeignTransferTransaction transaction) {
        this.foreignTransferTransaction = transaction;
        if (transaction.getSender() != this) {
            transaction.setSender(this);
        }
    }
}