package com.project.korex.batch.controller;

import com.project.korex.batch.service.ExchangeRateBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class ExchangeRateBatchController {

    private final ExchangeRateBatchService batchService;

    @PostMapping("/exchange-rate")
    public ResponseEntity<String> runExchangeRateBatch() {
        try {
            batchService.runExchangeRateBatch();
            return ResponseEntity.ok("환율 데이터 배치 작업이 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("배치 작업 시작 실패: " + e.getMessage());
        }
    }
}
