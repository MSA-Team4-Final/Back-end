package com.project.korex.user.exception;

import com.project.korex.common.code.ErrorCode;

public class EventNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public EventNotFoundException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
