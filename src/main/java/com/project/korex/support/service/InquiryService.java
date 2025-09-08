package com.project.korex.support.service;

import com.project.korex.common.code.ErrorCode;
import com.project.korex.support.dto.InquiryAnswerResponse;
import com.project.korex.support.dto.InquiryCreateRequest;
import com.project.korex.support.dto.InquiryResponse;
import com.project.korex.support.entity.Inquiry;
import com.project.korex.support.entity.InquiryAnswer;
import com.project.korex.support.enums.InquiryStatus;
import com.project.korex.support.exception.InquiryNotFoundException;
import com.project.korex.support.exception.InquiryWithdrawConflictException;
import com.project.korex.support.repository.jpa.InquiryAnswerJpaRepository;
import com.project.korex.support.repository.jpa.InquiryJpaRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class InquiryService {

    private final InquiryJpaRepository inquiryJpaRepository;
    private final InquiryAnswerJpaRepository inquiryAnswerJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Transactional
    public Long createInquiry(Long userId, InquiryCreateRequest req) {
        Users user = userJpaRepository.getReferenceById(userId);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .category(req.getCategory())
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .build();

        Inquiry saved = inquiryJpaRepository.save(inquiry);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        return inquiryJpaRepository
                .findByUserId(userId, pageable)
                .map(InquiryResponse::from);
    }

    @Transactional(readOnly = true)
    public InquiryResponse getMyInquirySummary(Long userId, Long inquiryId) {
        var inq = inquiryJpaRepository.findByIdAndUserId(inquiryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Inquiry not found"));
        return InquiryResponse.from(inq);
    }

    @Transactional
    public void withdrawMyInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryJpaRepository.findByIdAndUserIdForUpdate(inquiryId, userId)
                .orElseThrow(() -> new InquiryNotFoundException(ErrorCode.INQUIRY_NOT_FOUND));

        // 상태 검증
        if (Boolean.TRUE.equals(inquiry.getDeleted()) || inquiry.getStatus() != InquiryStatus.REGISTERED) {
            throw new InquiryWithdrawConflictException(ErrorCode.INQUIRY_WITHDRAW_CONFLICT);
        }

        inquiry.setStatus(InquiryStatus.WITHDRAW);
        inquiry.setDeleted(true);
    }

    public InquiryAnswerResponse getMyInquiryAnswer(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryJpaRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의를 찾을 수 없습니다."));

        if (!inquiry.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인 문의만 조회할 수 있습니다.");
        }

        InquiryAnswer answer = inquiryAnswerJpaRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("등록된 답변이 없습니다."));

        return InquiryAnswerResponse.from(answer);
    }

}
