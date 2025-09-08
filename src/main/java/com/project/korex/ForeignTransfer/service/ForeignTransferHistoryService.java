package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.response.ForeignTransferHistoryResponse;
import com.project.korex.ForeignTransfer.entity.ForeignTransferTransaction;
import com.project.korex.ForeignTransfer.entity.RecipientSnapshot;
import com.project.korex.ForeignTransfer.repository.ForeignTransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForeignTransferHistoryService {

    private final ForeignTransferTransactionRepository transactionRepository;

    public List<ForeignTransferHistoryResponse> getUserTransferHistory(String loginId) {
        List<ForeignTransferTransaction> transactions =
                transactionRepository.findAllByUser_LoginId(loginId);

        return transactions.stream().map(tx -> {
            RecipientSnapshot snapshot = tx.getRecipientSnapshot();

            // 계좌 종류 및 번호
            String accountType = null;
            String accountNumber = null;
            if (tx.getKrwNumber() != null) {
                accountType = "KRW";
                accountNumber = tx.getKrwNumber();
            } else if (tx.getForeignNumber() != null) {
                accountType = "FOREIGN";
                accountNumber = tx.getForeignNumber();
            }

            // 통화코드
            String senderCurrency = tx.getTransaction() != null ? tx.getTransaction().getFromCurrencyCode().getCode() : null;
            String recipientCurrency = snapshot != null ? snapshot.getCurrencyCode() : null;

            return ForeignTransferHistoryResponse.builder()
                    // 기본 거래 정보
                    .transferId(tx.getId())
                    .transactionId(tx.getTransaction() != null ? String.valueOf(tx.getTransaction().getId()) : null) // 변경
                    .accountType(accountType)
                    .transferStatus(tx.getTransferStatus().name())
                    .requestStatus(tx.getRequestStatus().name())
                    .createdAt(tx.getCreatedAt())
                    .agreedAt(tx.getTermsAgreement() != null ? tx.getTermsAgreement().getAgreedAt() : null)

                    // 송금 정보
                    .transferAmount(tx.getTransferAmount())
                    .convertedAmount(tx.getConvertedAmount())
                    .appliedRate(tx.getExchangeRate())
                    .feeAmount(tx.getTransaction() != null ? tx.getTransaction().getFeeAmount() : BigDecimal.ZERO)
                    .totalDeductedAmount(tx.getTransaction() != null ? tx.getTransaction().getTotalDeductedAmount() : BigDecimal.ZERO)
                    .transferReason(tx.getSender().getTransferReason())   // 송금 사유
                    .staffMessage(tx.getStaffMessage())       // 직원/관리자 메모

                    // 송금인 정보
                    .senderName(tx.getSender() != null ? tx.getSender().getName() : "정보 없음")
                    .senderCurrencyCode(senderCurrency)
                    .senderAccountNumber(accountNumber)
                    .senderCountry(tx.getSender() != null ? tx.getSender().getCountry() : null)
                    .senderAddress(tx.getSender() != null ? tx.getSender().getEngAddress() : null)
                    .senderCountryNumber(tx.getSender() != null ? tx.getSender().getCountryNumber() : null)
                    .senderPhoneNumber(tx.getSender() != null ? tx.getSender().getPhoneNumber() : null)
                    .senderEmail(tx.getSender() != null ? tx.getSender().getEmail() : null)

                    // 수취인 정보
                    .recipientId(snapshot != null ? snapshot.getRecipientId() : null)
                    .recipientName(snapshot != null ? snapshot.getName() : "정보 없음")
                    .recipientBank(snapshot != null ? snapshot.getBankName() : "정보 없음")
                    .recipientAccountNumber(snapshot != null ? snapshot.getAccountNumber() : "정보 없음")
                    .recipientCurrencyCode(recipientCurrency)
                    .recipientPhoneNumber(snapshot != null ? snapshot.getPhoneNumber() : null)
                    .recipientEmail(snapshot != null ? snapshot.getEmail() : "정보 없음")
                    .recipientCountry(snapshot != null ? snapshot.getCountry() : null)
                    .recipientAddress(snapshot != null ? snapshot.getEngAddress() : null)
                    .relationRecipient(tx.getRelationRecipient())

                    .build();
        }).toList();
    }
}
