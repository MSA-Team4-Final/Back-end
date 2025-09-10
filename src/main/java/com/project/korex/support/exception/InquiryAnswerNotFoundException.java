package com.project.korex.support.exception;

import com.project.korex.common.code.ErrorCode;

public class InquiryAnswerNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public InquiryAnswerNotFoundException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
