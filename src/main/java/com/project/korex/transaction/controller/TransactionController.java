package com.project.korex.transaction.controller;

import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.externalAccount.dto.response.DepositWithdrawHistoryDto;
import com.project.korex.externalAccount.dto.response.DepositWithdrawSummaryDto;
import com.project.korex.transaction.dto.response.MonthlyStatsDto;
import com.project.korex.transaction.dto.response.TransactionResponseDto;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/history/{userId}")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "date") String sortBy) {

        try {
            // 인증 확인
//            if (userPrincipal == null || userPrincipal.getUser() == null) {
//                Map<String, Object> errorResponse = new HashMap<>();
//                errorResponse.put("success", false);
//                errorResponse.put("message", "인증되지 않은 사용자입니다.");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//            }
//
//            // 현재 로그인한 사용자와 요청한 userId가 일치하는지 확인 (보안 체크)
//            Long authenticatedUserId = userPrincipal.getUser().getId();
//            if (!authenticatedUserId.equals(userId)) {
//                Map<String, Object> errorResponse = new HashMap<>();
//                errorResponse.put("success", false);
//                errorResponse.put("message", "권한이 없습니다.");
//                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
//            }

            List<TransactionResponseDto> transactions = transactionService.getUserTransactions(
                    userId, currencyCode, period, type, sortBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "거래 내역 조회 성공");
            response.put("transactions", transactions);
            response.put("timestamp", System.currentTimeMillis());

            log.info("거래 내역 조회 성공 - userId: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("거래 내역 조회 실패 - userId: {}, error: {}", userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "거래 내역 조회 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/monthly-stats/{userId}")
    public ResponseEntity<Map<String, Object>> getMonthlyStats(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam String currencyCode) {

        try{
            MonthlyStatsDto stats = transactionService.getMonthlyStats(userId, currencyCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "월간 통계 조회 성공");
            response.put("stats", stats);
            response.put("timestamp", System.currentTimeMillis());

            log.info("월간 통계 조회 성공 - userId: {}, currencyCode: {}", userId, currencyCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("월간 통계 조회 실패 - userId: {}, currencyCode: {}, error: {}",
                    userId, currencyCode, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "월간 통계 조회 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * 충전/출금 내역 조회
     */
    @GetMapping("/deposit-withdraw-history")
    public ResponseEntity<List<DepositWithdrawHistoryDto>> getDepositWithdrawHistory(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getUserId();

            log.info("충전/출금 내역 조회 요청 - userId: {}, type: {}, startDate: {}, endDate: {}",
                    userId, type, startDate, endDate);

            List<DepositWithdrawHistoryDto> history = transactionService
                    .getDepositwithdrawHistory(userId, type, startDate, endDate);

            log.info("충전/출금 내역 조회 완료 - 건수: {}", history.size());

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("충전/출금 내역 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 충전/출금 요약 정보 조회
     */
    @GetMapping("/deposit-withdraw-summary")
    public ResponseEntity<DepositWithdrawSummaryDto> getDepositWithdrawSummary(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getUserId();

            log.info("충전/출금 요약 정보 조회 요청 - userId: {}, type: {}, startDate: {}, endDate: {}",
                    userId, type, startDate, endDate);

            DepositWithdrawSummaryDto summary = transactionService
                    .getDepositwithdrawSummary(userId, type, startDate, endDate);

            log.info("충전/출금 요약 정보 조회 완료");

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("충전/출금 요약 정보 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
