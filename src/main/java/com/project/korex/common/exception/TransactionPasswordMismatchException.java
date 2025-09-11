package com.project.korex.common.exception;

import com.project.korex.common.code.ErrorCode;
import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;

@Getter
public class TransactionPasswordMismatchException extends BadCredentialsException {

    private final ErrorCode errorCode;

    public TransactionPasswordMismatchException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
