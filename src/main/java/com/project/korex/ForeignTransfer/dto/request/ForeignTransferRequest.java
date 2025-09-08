package com.project.korex.ForeignTransfer.dto.request;

import com.project.korex.transaction.enums.AccountType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
public class ForeignTransferRequest {

    // 1. 송금 금액/계좌
    private BigDecimal transferAmount;     // 입력 금액
    private AccountType accountType;       // 계좌 유형
    private String currencyCode;           // 송금 통화
    private String accountPassword;        // 계좌 비밀번호
    private String krwAccount;             // 출금 계좌
    private String foreignAccount;         // 수취인 계좌
    private BigDecimal exchangeRate;

    // 2. 송금인 정보
    private String senderName;
    private String transferReason;
    private String countryNumber;
    private String phoneNumber;
    private String email;
    private String country;
    private String engAddress;
    private String relationRecipient;
    private String accountNumber;          // 송금인 계좌번호
    private String withdrawalMethod;       // 출금 방식
    private String staffMessage;

    // 3. 첨부 파일
    private MultipartFile idFile;               // 신분증
    private MultipartFile proofDocumentFile;    // 송금 사유 증빙
    private MultipartFile relationDocumentFile; // 관계 증빙

    // 4. 약관 동의
    private boolean agree1;
    private boolean agree2;
    private boolean agree3;

    // 5. 추가 계산용 필드 (프론트에서 필요 시)
    private BigDecimal totalAmountKRW;      // 원화 기준 총 금액
    private BigDecimal convertedAmount;     // 환전 금액
    private BigDecimal feeAmount;           // 수수료

    // 6. 수취인 정보
    private Long recipientId;               // 🔹 새로 추가
    private String recipientName;
    private String recipientBank;
    private String recipientAccountNumber;
    private String recipientEmail;
}
