package com.project.korex.account.exception;

import com.project.korex.common.code.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidAccountPasswordException extends RuntimeException {

    private final ErrorCode errorCode;

    public InvalidAccountPasswordException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}