package com.project.korex.ForeignTransfer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ForeignTransferHistoryResponse {
    // 기본 거래 정보
    private Long transferId;
    private String transactionId;             // 거래 고유 ID
    private String accountType;               // 출금 계좌 종류
    private String transferStatus;            // 송금 상태
    private String requestStatus;             // 요청 상태
    private LocalDateTime createdAt;          // 생성일
    private LocalDateTime agreedAt;           // 약관 동의일

    // 송금 정보
    private BigDecimal transferAmount;        // 송금 원화 금액
    private BigDecimal convertedAmount;       // 환전 후 금액
    private BigDecimal appliedRate;           // 적용 환율
    private BigDecimal feeAmount;             // 수수료
    private BigDecimal totalDeductedAmount;   // 총 차감 금액
    private String transferReason;            // 송금 사유
    private String staffMessage;              // 직원/관리자 메모

    // 송금인 정보
    private String senderName;
    private String senderCurrencyCode;
    private String senderAccountNumber;
    private String senderCountry;             // 송금인 국가
    private String senderAddress;             // 송금인 주소
    private String senderCountryNumber;       // 송금인 국가번호 (전화)
    private String senderPhoneNumber;         // 송금인 전화번호
    private String senderEmail;               // 송금인 이메일

    // 수취인 정보 (스냅샷)
    private Long recipientId;
    private String recipientName;
    private String recipientBank;
    private String recipientAccountNumber;
    private String recipientCurrencyCode;
    private String recipientPhoneNumber;
    private String recipientEmail;
    private String recipientCountry;          // 수취인 국가
    private String recipientAddress;          // 수취인 주소
    private String relationRecipient;         // 송금인과의 관계
}
