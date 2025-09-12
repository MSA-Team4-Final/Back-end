package com.project.korex.ForeignTransfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ForeignTransferTermsAgreement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "transfer_id")
    private ForeignTransferTransaction foreignTransferTransaction;

    @Column(name = "agree1")
    private Boolean agree1; // 개인정보 제3자 제공 동의

    @Column(name = "agree2")
    private Boolean agree2; // 개인정보 수집 및 이용

    @Column(name = "agree3")
    private Boolean agree3; // 해외 송금 이용약관

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;
}
