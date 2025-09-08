package com.project.korex.externalAccount.entity;

import com.project.korex.externalAccount.enums.AccountRole;
import com.project.korex.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "transaction_external_account")
public class TransactionExternalAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_account_id", nullable = true)
    private ExternalAccount externalAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role")
    private AccountRole accountRole;


}

