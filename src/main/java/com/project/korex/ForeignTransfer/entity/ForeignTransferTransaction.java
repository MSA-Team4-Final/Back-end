package com.project.korex.ForeignTransfer.entity;

import com.project.korex.ForeignTransfer.enums.RequestStatus;
import com.project.korex.ForeignTransfer.enums.TransferStatus;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ForeignTransferTransaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ForeignTransferTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @OneToOne(mappedBy = "foreignTransferTransaction", cascade = CascadeType.ALL)
    private Sender sender;

    @OneToOne(mappedBy = "foreignTransferTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private TermsAgreement termsAgreement;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "transfer_amount", precision = 18, scale = 4)
    private BigDecimal transferAmount;

    @Column(name = "krw_number")
    private String krwNumber;

    @Column(name = "foreign_number")
    private String foreignNumber;

    @Column(name = "converted_amount", precision = 18, scale = 4)
    private BigDecimal convertedAmount;

    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "staff_message")
    private String staffMessage;

    @Column(name = "relation_recipient")
    private String relationRecipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status")
    private RequestStatus requestStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_status")
    private TransferStatus transferStatus;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private RecipientSnapshot recipientSnapshot;

    @Column(name = "recipient_id")
    private Long recipientId;

    // 편의 메서드: Sender와 양방향 관계 설정
    public void setSender(Sender sender) {
        this.sender = sender;
        if (sender.getForeignTransferTransaction() != this) {
            sender.setForeignTransferTransaction(this);
        }
    }
}