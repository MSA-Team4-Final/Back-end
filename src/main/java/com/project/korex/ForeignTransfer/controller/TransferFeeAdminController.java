package com.project.korex.ForeignTransfer.controller;

import com.project.korex.ForeignTransfer.entity.TransferFeeAdmin;
import com.project.korex.ForeignTransfer.service.TransferFeeAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/fee")
public class TransferFeeAdminController {

    private final TransferFeeAdminService service;

    public TransferFeeAdminController(TransferFeeAdminService service) {
        this.service = service;
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<TransferFeeAdmin>> getAllFeePolicies() {
        return ResponseEntity.ok(service.getAllPolicies());
    }

    // 등록 / 수정
    @PostMapping
    public ResponseEntity<TransferFeeAdmin> saveOrUpdatePolicy(@RequestBody TransferFeeAdmin policy) {
        TransferFeeAdmin saved = service.saveOrUpdatePolicy(policy);
        return ResponseEntity.ok(saved);
    }

    // 수정 (존재하면 덮어쓰기)
    @PutMapping("/{currencyCode}")
    public ResponseEntity<TransferFeeAdmin> updatePolicy(
            @PathVariable String currencyCode,
            @RequestBody TransferFeeAdmin policy
    ) {
        policy.setCurrencyCode(currencyCode);
        TransferFeeAdmin updated = service.saveOrUpdatePolicy(policy);
        return ResponseEntity.ok(updated);
    }

}
