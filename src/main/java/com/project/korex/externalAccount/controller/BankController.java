package com.project.korex.externalAccount.controller;

import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.externalAccount.dto.response.BankResponseDto;
import com.project.korex.externalAccount.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
@Slf4j
public class BankController {

    private final BankService bankService;

    // 기존 메서드 - 모든 은행
    @GetMapping
    public ResponseEntity<List<BankResponseDto>> getAllBanks() {
        List<BankResponseDto> banks = bankService.getAllBanks();
        return ResponseEntity.ok(banks);
    }

    /**
     * 모든 은행 목록 조회
     */
    @GetMapping("/my-banks")
    public ResponseEntity<List<BankResponseDto>> getMyActiveBanks(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal) {
        Long userId = customUserPrincipal.getUserId(); // JWT에서 사용자 ID 추출
        List<BankResponseDto> banks = bankService.getUserActiveBanks(userId);
        log.info("사용자 활성 은행 조회 완료 - userId: {}, 결과: {}개", userId, banks.size());
        return ResponseEntity.ok(banks);
    }

    /**
     * 특정 은행 정보 조회
     */
    @GetMapping("/{bankCode}")
    public ResponseEntity<BankResponseDto> getBankByCode(@PathVariable String bankCode) {
        BankResponseDto bank = bankService.getBankByCode(bankCode);
        log.info("은행 정보 조회 완료 - bankCode: {}", bankCode);
        return ResponseEntity.ok(bank);
    }

    /**
     * 은행명으로 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<BankResponseDto>> searchBanks(
            @RequestParam(name = "name") String bankName) {
        List<BankResponseDto> banks = bankService.searchBanksByName(bankName);
        log.info("은행 검색 완료 - keyword: {}, 결과: {}개", bankName, banks.size());
        return ResponseEntity.ok(banks);
    }

    /**
     * 은행 코드 유효성 검증
     */
    @GetMapping("/{bankCode}/exists")
    public ResponseEntity<Boolean> checkBankCodeExists(@PathVariable String bankCode) {
        boolean exists = bankService.isValidBankCode(bankCode);
        return ResponseEntity.ok(exists);
    }
}

