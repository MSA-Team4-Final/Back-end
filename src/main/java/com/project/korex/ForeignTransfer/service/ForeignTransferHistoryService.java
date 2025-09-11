package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.response.ForeignTransferHistoryResponse;
import com.project.korex.ForeignTransfer.entity.ForeignTransferTransaction;
import com.project.korex.ForeignTransfer.entity.RecipientSnapshot;
import com.project.korex.ForeignTransfer.repository.ForeignTransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForeignTransferHistoryService {

    private final ForeignTransferTransactionRepository transactionRepository;

    public List<ForeignTransferHistoryResponse> getUserTransferHistory(
            String loginId,
            LocalDate startDate,
            LocalDate endDate) {

        // 기간 필터링
        List<ForeignTransferTransaction> transactions;
        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findAllByUser_LoginIdAndCreatedAtBetween(
                    loginId,
                    startDate.atStartOfDay(),
                    endDate.atTime(23,59,59));
        } else {
            transactions = transactionRepository.findAllByUser_LoginId(loginId);
        }

        // 나머지는 기존 코드 그대로
        return transactions.stream().map(tx -> {
            RecipientSnapshot snapshot = tx.getRecipientSnapshot();

            String accountType = null;
            String accountNumber = null;
            if (tx.getKrwNumber() != null && !tx.getKrwNumber().isBlank()) {
                accountType = "KRW";
                accountNumber = tx.getKrwNumber();
            } else if (tx.getForeignNumber() != null && !tx.getForeignNumber().isBlank()) {
                accountType = "FOREIGN";
                accountNumber = tx.getForeignNumber();
            }

            String senderCurrency = null;
            if (tx.getTransaction() != null && tx.getTransaction().getFromCurrencyCode() != null) {
                try {
                    senderCurrency = tx.getTransaction().getFromCurrencyCode().getCode();
                } catch (Exception e) {
                    senderCurrency = null;
                }
            }

            BigDecimal feeAmount = BigDecimal.ZERO;
            BigDecimal totalDeducted = BigDecimal.ZERO;
            if (tx.getTransaction() != null) {
                if (tx.getTransaction().getFeeAmount() != null) feeAmount = tx.getTransaction().getFeeAmount();
                if (tx.getTransaction().getTotalDeductedAmount() != null) totalDeducted = tx.getTransaction().getTotalDeductedAmount();
            }

            String senderName = tx.getSender() != null ? tx.getSender().getName() : "정보 없음";
            String senderCountry = tx.getSender() != null ? tx.getSender().getCountry() : null;
            String senderAddress = tx.getSender() != null ? tx.getSender().getEngAddress() : null;
            String senderEmail = tx.getSender() != null ? tx.getSender().getEmail() : null;
            String senderCountryNumber = tx.getSender() != null ? tx.getSender().getCountryNumber() : null;
            String senderPhoneNumber = tx.getSender() != null ? tx.getSender().getPhoneNumber() : null;

            String recipientName = snapshot != null ? snapshot.getName() : "정보 없음";
            String recipientBank = snapshot != null ? snapshot.getBankName() : "정보 없음";
            String recipientAccountNumber = snapshot != null ? snapshot.getAccountNumber() : "정보 없음";
            String recipientEmail = snapshot != null ? snapshot.getEmail() : "정보 없음";
            String recipientCurrencyCode = snapshot != null ? snapshot.getCurrencyCode() : null;
            String recipientCountry = snapshot != null ? snapshot.getCountry() : null;
            String recipientAddress = snapshot != null ? snapshot.getEngAddress() : null;
            String recipientCountryNumber = snapshot != null ? snapshot.getCountryNumber() : null;
            String recipientPhoneNumber = snapshot != null ? snapshot.getPhoneNumber() : null;

            return ForeignTransferHistoryResponse.builder()
                    .transferId(tx.getId())
                    .transactionId(tx.getTransaction() != null ? String.valueOf(tx.getTransaction().getId()) : null)
                    .accountType(accountType)
                    .transferStatus(tx.getTransferStatus() != null ? tx.getTransferStatus().name() : null)
                    .requestStatus(tx.getRequestStatus() != null ? tx.getRequestStatus().name() : null)
                    .createdAt(tx.getCreatedAt())
                    .agreedAt(tx.getTermsAgreement() != null ? tx.getTermsAgreement().getAgreedAt() : null)

                    .transferAmount(tx.getTransferAmount())
                    .convertedAmount(tx.getConvertedAmount())
                    .appliedRate(tx.getExchangeRate())
                    .feeAmount(feeAmount)
                    .totalDeductedAmount(totalDeducted)
                    .transferReason(tx.getSender() != null ? tx.getSender().getTransferReason() : null)
                    .staffMessage(tx.getStaffMessage())

                    .senderName(senderName)
                    .senderCurrencyCode(senderCurrency)
                    .senderAccountNumber(accountNumber)
                    .senderCountry(senderCountry)
                    .senderAddress(senderAddress)
                    .senderEmail(senderEmail)
                    .senderCountryNumber(senderCountryNumber)
                    .senderPhoneNumber(senderPhoneNumber)

                    .recipientName(recipientName)
                    .recipientBank(recipientBank)
                    .recipientAccountNumber(recipientAccountNumber)
                    .recipientCurrencyCode(recipientCurrencyCode)
                    .recipientEmail(recipientEmail)
                    .recipientCountry(recipientCountry)
                    .recipientAddress(recipientAddress)
                    .recipientCountryNumber(recipientCountryNumber)
                    .recipientPhoneNumber(recipientPhoneNumber)
                    .relationRecipient(tx.getRelationRecipient())

                    .build();
        }).toList();
    }
}