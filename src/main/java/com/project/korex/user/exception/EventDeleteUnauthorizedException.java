package com.project.korex.user.exception;

import com.project.korex.common.code.ErrorCode;

public class EventDeleteUnauthorizedException extends RuntimeException {
  private final ErrorCode errorCode;

  public EventDeleteUnauthorizedException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
