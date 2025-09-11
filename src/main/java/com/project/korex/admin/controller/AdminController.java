package com.project.korex.admin.controller;

import com.project.korex.admin.dto.*;
import com.project.korex.admin.service.AdminService;
import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.support.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "Admin API")
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RestController
public class AdminController {

    private final AdminService adminService;

    // 대시보드
    @GetMapping("/metrics")
    public MetricsResponseDto getMetrics(
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end) {
        LocalDateTime startLdt = start.toLocalDateTime();
        LocalDateTime endLdt = end.toLocalDateTime();
        return adminService.getMetrics(startLdt, endLdt);
    }

    @GetMapping("/restricted-latest")
    public List<RestrictedUserDto> getLatestRestrictedUsers(@RequestParam(defaultValue = "3") int size) {
        return adminService.getLatestRestrictedUsers(size);
    }

    // 회원 관리
    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getUsers() {
        List<UserResponseDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{loginId}/unlock")
    public ResponseEntity<String> unlockUser(@PathVariable String loginId) {
        adminService.unlockUser(loginId);
        return ResponseEntity.ok("계정 잠금 해제 완료");
    }

    @PostMapping("/{loginId}/lock")
    public ResponseEntity<String> lockUser(@PathVariable String loginId) {
        adminService.lockUser(loginId);
        return ResponseEntity.ok("계정 잠금 완료");
    }

    // 문의 관리
    @GetMapping("/inquiries")
    public ResponseEntity<List<InquiryResponseDto>> getInquiries() {
        List<InquiryResponseDto> inquiries = adminService.getAllInquiries();
        return ResponseEntity.ok(inquiries);
    }

    @PostMapping("/inquiries/{id}/answer")
    public ResponseEntity<InquiryAnswerResponseDto> writeInquiryAnswer(
            @PathVariable Long id,
            @RequestBody InquiryAnswerRequestDto dto,
            @AuthenticationPrincipal CustomUserPrincipal adminId) {

        dto.setInquiryId(id);

        Long adminIdValue = adminId.getUser().getId();
        InquiryAnswerResponseDto response = adminService.writeInquiryAnswer(
                adminIdValue, dto
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inquiries/filter")
    public ResponseEntity<List<InquiryResponseDto>> getNotAnswerInquiries() {
        InquiryStatus status = InquiryStatus.REGISTERED;
        List<InquiryResponseDto> inquiries = adminService.getNotAnswerInquiries(status);
        return ResponseEntity.ok(inquiries);
    }

    //Todo: 해외송금 관리
}
