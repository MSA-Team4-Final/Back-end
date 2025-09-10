package com.project.korex.ForeignTransfer.controller;

import com.project.korex.ForeignTransfer.dto.request.ForeignTransferRequest;
import com.project.korex.ForeignTransfer.dto.request.TransferExchangeRequest;
import com.project.korex.ForeignTransfer.dto.response.ForeignTransferHistoryResponse;
import com.project.korex.ForeignTransfer.dto.response.ForeignTransferResponse;
import com.project.korex.ForeignTransfer.dto.response.TransferExchangeResponse;
import com.project.korex.ForeignTransfer.service.ForeignTransferExchangePreviewService;
import com.project.korex.ForeignTransfer.service.ForeignTransferExchangeService;
import com.project.korex.ForeignTransfer.service.ForeignTransferHistoryService;
import com.project.korex.ForeignTransfer.service.ForeignTransferService;
import com.project.korex.common.security.user.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/foreign-transfer")
@RequiredArgsConstructor
public class ForeignTransferController {

    private final ForeignTransferService foreignTransferService;
    private final ForeignTransferExchangeService foreignTransferExchangeService;
    private final ForeignTransferExchangePreviewService previewService;
    private final ForeignTransferHistoryService historyService;

    /**
     * 실제 외화 송금 요청 처리
     */
    @PostMapping("/request")
    public ResponseEntity<ForeignTransferResponse> createForeignTransfer(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestPart("request") ForeignTransferRequest request,
            @RequestPart(value = "idFile", required = false) MultipartFile idFile,
            @RequestPart(value = "proofDocumentFile", required = false) MultipartFile proofDocumentFile,
            @RequestPart(value = "relationDocumentFile", required = false) MultipartFile relationDocumentFile
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        ForeignTransferResponse response = foreignTransferService.processFullForeignTransfer(
                principal.getName(),
                request,
                idFile,
                proofDocumentFile,
                relationDocumentFile
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 프론트용 미리보기 환율/수수료 계산
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewExchange(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam BigDecimal amount
    ) {
        try {
            TransferExchangeResponse response = previewService.simulatePreview(fromCurrency, toCurrency, amount);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("환율 미리보기 계산 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public List<ForeignTransferHistoryResponse> getUserTransferHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        String loginId = principal.getName();
        return historyService.getUserTransferHistory(loginId);
    }
}
