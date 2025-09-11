package com.project.korex.common.advice;

import com.project.korex.auth.exception.*;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.dto.ErrorResponseDto;
import com.project.korex.common.exception.InsufficientBalanceException;
import com.project.korex.common.exception.TransactionPasswordMismatchException;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.support.exception.InquiryAnswerNotFoundException;
import com.project.korex.transaction.exception.CannotTransferToSelfException;
import com.project.korex.support.exception.InquiryWithdrawConflictException;
import com.project.korex.support.exception.InquiryNotFoundException;
import com.project.korex.transaction.exception.ExchangeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 NOT_FOUND - 리소스를 찾을 수 없음
    @ExceptionHandler({
            UserNotFoundException.class,
            RoleNotFoundException.class,
            InquiryNotFoundException.class,
            InquiryAnswerNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(RuntimeException ex, HttpServletRequest request) {
        ErrorCode errorCode = getErrorCodeFromException(ex);

        log.warn("[NotFoundException] {} - Path: {}", errorCode.getMessage(), request.getRequestURI());

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // 409 CONFLICT - 중복/충돌
    @ExceptionHandler({
            DuplicateLoginIdException.class,
            DuplicateEmailException.class,
            InquiryWithdrawConflictException.class
    })
    public ResponseEntity<ErrorResponseDto> handleConflictException(RuntimeException ex, HttpServletRequest request) {
        ErrorCode errorCode = getErrorCodeFromException(ex);

        log.warn("[ConflictException] {} - Path: {}", errorCode.getMessage(), request.getRequestURI());

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // 400 BAD_REQUEST - 잘못된 요청/비즈니스 로직 위반
    @ExceptionHandler({
            PasswordMismatchException.class,
            TokenNotFoundException.class,
            VerificationTokenNotFoundException.class,
            InvalidVerificationCodeException.class,
            EmailNotVerifiedException.class,
            InsufficientBalanceException.class,
            CannotTransferToSelfException.class,
            ExchangeException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequestException(RuntimeException ex, HttpServletRequest request) {
        ErrorCode errorCode = getErrorCodeFromException(ex);

        log.warn("[BadRequestException] {} - Path: {}", errorCode.getMessage(), request.getRequestURI());

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // 401 UNAUTHORIZED - 인증 실패
    @ExceptionHandler({
            InvalidTokenException.class,
            TokenExpriedException.class,
            TransactionPasswordMismatchException.class
    })
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(RuntimeException ex, HttpServletRequest request) {
        ErrorCode errorCode = getErrorCodeFromException(ex);

        log.warn("[UnauthorizedException] {} - Path: {}", errorCode.getMessage(), request.getRequestURI());

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // Validation 오류 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        log.warn("[ValidationException] {} - Path: {}", errorCode.getMessage(), request.getRequestURI());

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI(), ex.getBindingResult().getFieldErrors());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleLoginFailed(LoginFailedException ex,
                                                              HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode(); // 예: U004 / BAD_REQUEST

        // ErrorResponseDto에 선택 필드(failCount, restricted)만 채워서 내려주기
        ErrorResponseDto body = ErrorResponseDto.of(
                errorCode,
                request.getRequestURI(),
                ex.getFailCount(),
                ex.getRestricted()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .header("X-Fail-Count",
                        ex.getFailCount() == null ? "" : String.valueOf(ex.getFailCount()))
                .body(body);
    }

    // 전체 예외 처리 (최후 수단)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleInternalServerError(Exception ex, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error("서버 오류 발생: {}", ex.getMessage(), ex);

        ErrorResponseDto response = ErrorResponseDto.of(errorCode, request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }


    // ErrorCode 추출 헬퍼 메서드
    private ErrorCode getErrorCodeFromException(RuntimeException ex) {
        try {
            return (ErrorCode) ex.getClass().getMethod("getErrorCode").invoke(ex);
        } catch (Exception e) {
            log.error("ErrorCode 추출 실패: {}", ex.getClass().getSimpleName());
            return ErrorCode.INTERNAL_SERVER_ERROR;
        }
    }
}
