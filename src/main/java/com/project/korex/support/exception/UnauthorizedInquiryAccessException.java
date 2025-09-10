package com.project.korex.support.exception;

import com.project.korex.common.code.ErrorCode;

public class UnauthorizedInquiryAccessException extends RuntimeException {
  private final ErrorCode errorCode;

  public UnauthorizedInquiryAccessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
