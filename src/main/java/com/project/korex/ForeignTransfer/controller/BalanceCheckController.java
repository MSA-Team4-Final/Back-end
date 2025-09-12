    package com.project.korex.ForeignTransfer.controller;

    import com.project.korex.ForeignTransfer.dto.response.BalanceCheckResponse;
    import com.project.korex.ForeignTransfer.service.BalanceCheckService;
    import com.project.korex.common.security.user.CustomUserPrincipal;
    import com.project.korex.transaction.enums.AccountType;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/foreign-transfer")
    @RequiredArgsConstructor
    public class BalanceCheckController {

        private final BalanceCheckService balanceCheckService;

        // ----------------- 전체 계좌 조회 -----------------
        @GetMapping("/balances")
        public ResponseEntity<BalanceCheckResponse> getAllBalances(
                @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();
            BalanceCheckResponse response = balanceCheckService.checkAllBalances(loginId);
            return ResponseEntity.ok(response);
        }

        // ----------------- 단일 계좌 조회 -----------------
        @GetMapping("/balance")
        public ResponseEntity<BalanceCheckResponse> getBalance(
                @RequestParam("accountType") String accountTypeStr,
                @RequestParam(value = "currencyCode", required = false) String currencyCode,
                @AuthenticationPrincipal CustomUserPrincipal principal
        ) {
            String loginId = principal.getName();

            AccountType accountType;
            try {
                accountType = AccountType.valueOf(accountTypeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }

            BalanceCheckResponse response = balanceCheckService.checkBalance(
                    loginId,
                    accountType,
                    currencyCode
            );
            return ResponseEntity.ok(response);
        }
    }
