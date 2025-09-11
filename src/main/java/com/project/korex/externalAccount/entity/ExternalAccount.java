package com.project.korex.externalAccount.entity;

import com.project.korex.common.BaseEntity;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "external_account")
@Getter
@Setter
@NoArgsConstructor
public class ExternalAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user; // 기존 Users 테이블과 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_code")
    private Bank bankCode; // Bank 테이블과 연결

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String accountHolder;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal simulationBalance = BigDecimal.ZERO; // 시뮬레이션용 잔액

    @Column(nullable = false)
    private Boolean isPrimary = false; // 주계좌 여부

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
//    @OneToMany(mappedBy = "transaction_external_account", cascade = CascadeType.ALL)
//    private List<TransactionExternalAccount> transactions = new ArrayList<>();
}

