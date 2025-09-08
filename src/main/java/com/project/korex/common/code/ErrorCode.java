package com.project.korex.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (Cxxx)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),

    // User (Uxxx)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 회원을 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "u002", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U003", "이미 사용 중인 이메일입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U004", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "u005", "이미 사용 중인 번호입니다."),

    // Auth (Axxx)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A003", "인증에 실패했습니다. 유효한 자격 증명이 필요합니다."),
    TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "A004", "액세스 토큰이 필요합니다."),
    VERIFICATION_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "A005", "이메일 인증 토큰이 존재하지 않거나 만료되었습니다."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "A006", "인증 코드가 일치하지 않습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "A007", "이메일 인증 정보가 존재하지 않습니다."),

    // Role (RXXX)
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "역할을 찾을 수 없습니다."),

    // Support (SXXX)
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "문의 내역을 찾을 수 없습니다."),
    INQUIRY_WITHDRAW_CONFLICT(HttpStatus.CONFLICT, "S002", "문의 철회가 불가능한 상태입니다."),

    // Balance
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST,"B001", "잔액이 부족합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "해당 통화가 없습니다."),
   TRANSFER_NOT_SELF(HttpStatus.BAD_REQUEST, "B003", "본인에게 송금할 수 없습니다."),

    // ForeignTransfer
    CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "해당 통화를 찾을 수 없습니다."),
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "수취인 정보를 찾을 수 없습니다."),

    // Exchange
    INVALID_FROM_CURRENCY(HttpStatus.UNAUTHORIZED, "E001", "유효하지 않은 출금 통화입니다."),
    INVALID_TO_CURRENCY(HttpStatus.UNAUTHORIZED, "E002", "유효하지 않은 입금 통화입니다."),
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.NOT_FOUND,"E003", "환율 정보를 조회 할 수 없습니다."),

    // Favorite
    INVALID_REQUEST(HttpStatus.UNAUTHORIZED, "F001", "본인은 즐겨찾기에 추가할 수 없습니다."),
    DUPLICATE_FAVORITE(HttpStatus.CONFLICT, "F002", "이미 즐겨찾기에 등록된 친구입니다"),
    FAVORITE_LIMIT_EXCEED(HttpStatus.CONFLICT, "F003", "즐겨찾기는 최대 4명까지만 등록 가능합니다"),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "F004", "즐겨찾기를 찾을 수 없습니다"),

    // Account
    INVALID_ACCOUNT_PASSWORD(HttpStatus.BAD_REQUEST, "AC01", "계좌 비밀번호가 유효하지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
