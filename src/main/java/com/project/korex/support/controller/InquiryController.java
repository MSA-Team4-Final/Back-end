package com.project.korex.support.controller;

import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.support.dto.InquiryAnswerResponse;
import com.project.korex.support.dto.InquiryCreateRequest;
import com.project.korex.support.dto.InquiryResponse;
import com.project.korex.support.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@RestController
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 등록
     */
    @PostMapping("/create")
    public ResponseEntity<Long> createInquiry(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody InquiryCreateRequest req) {
        Long userId = principal.getUser().getId();

        Long id = inquiryService.createInquiry(userId, req);
        return ResponseEntity.ok(id);
    }

    /**
     * 문의 조회(List)
     */
    @GetMapping("/list")
    public ResponseEntity<Page<InquiryResponse>> getMyInquiries(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Long userId = principal.getUser().getId();
        Page<InquiryResponse> page = inquiryService.getMyInquiries(userId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 문의 조회(상세)
     */
    @GetMapping("/{id}")
    public ResponseEntity<InquiryResponse> getMyInquirySummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id) {
        Long userId = principal.getUser().getId();
        InquiryResponse res = inquiryService.getMyInquirySummary(userId, id);
        return ResponseEntity.ok(res);
    }

    /**
     * 문의 철회(관리자 답변 작성 전)
     */
    @PostMapping("/{id}/withdraw")
    public void withdrawMyInquiry(@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long id) {
        Long userId = principal.getUser().getId();
        inquiryService.withdrawMyInquiry(userId, id);

    }

    /**
     * 문의 답변 조회
     */
    @GetMapping("/{id}/answer")
    public ResponseEntity<InquiryAnswerResponse> getInquiryAnswer(
                                @PathVariable("id") Long inquiryId,
                                @AuthenticationPrincipal CustomUserPrincipal principal) {
        InquiryAnswerResponse response = inquiryService.getMyInquiryAnswer(inquiryId, principal.getUser().getId());
        return ResponseEntity.ok(response);
    }

}
