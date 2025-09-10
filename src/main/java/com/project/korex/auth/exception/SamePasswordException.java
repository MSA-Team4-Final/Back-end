package com.project.korex.auth.exception;

import com.project.korex.common.code.ErrorCode;

public class SamePasswordException extends RuntimeException {
  private final ErrorCode errorCode;

  public SamePasswordException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
