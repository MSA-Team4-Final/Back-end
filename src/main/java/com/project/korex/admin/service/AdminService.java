package com.project.korex.admin.service;

import com.project.korex.admin.dto.*;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.support.entity.Inquiry;
import com.project.korex.support.entity.InquiryAnswer;
import com.project.korex.support.enums.InquiryStatus;
import com.project.korex.support.exception.InquiryNotFoundException;
import com.project.korex.support.repository.jpa.InquiryAnswerJpaRepository;
import com.project.korex.support.repository.jpa.InquiryJpaRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserJpaRepository userJpaRepository;
    private final InquiryJpaRepository inquiryJpaRepository;
    private final InquiryAnswerJpaRepository inquiryAnswerJpaRepository;

    public MetricsResponseDto getMetrics(LocalDateTime start, LocalDateTime end) {
        long newUsers = userJpaRepository.countByCreatedAtBetween(start, end);
        long activeUsers = userJpaRepository.countByRestrictedFalseAndCreatedAtBetween(start, end);
        long restrictedUsers = userJpaRepository.countByRestrictedTrueAndRestrictedAtBetween(start, end);
        // 미처리 송금 갯수 추가
        //long pendingRemittances =
        InquiryStatus status = InquiryStatus.REGISTERED;
        long pendingInquiries = inquiryJpaRepository.countByStatusAndCreatedAtBetween(status, start, end);

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("newUsers", newUsers);
        metrics.put("activeUsers", activeUsers);
        metrics.put("restrictedUsers", restrictedUsers);
        metrics.put("pendingInquiries", pendingInquiries);

        return new MetricsResponseDto(metrics);
    }

    public List<RestrictedUserDto> getLatestRestrictedUsers(int size) {
        List<Users> users = userJpaRepository.findTop3ByRestrictedOrderByRestrictedAtDesc(true);
        System.out.println("제한 회원 아이디: " + users.get(0).getId());

        return users.stream()
                .map(u -> new RestrictedUserDto(
                        u.getId(),
                        u.getEmail(),
                        "FAILED_LOGIN_THRESHOLD",
                        u.getRestrictedAt()))
                .collect(Collectors.toList());
    }

   @Transactional
    public List<UserResponseDto> getAllUsers() {
        return userJpaRepository.findAll()
                .stream()
                .filter(user -> !"ROLE_ADMIN".equals(user.getRole().getRoleName()))
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .loginId(user.getLoginId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .birth(user.getBirth())
                        .status(user.isRestricted()? "제한" : "활성")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void unlockUser(String loginId) {
        Users user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (!user.isRestricted()) {
            throw new IllegalStateException("이미 잠금 해제된 계정입니다.");
        }

        user.setRestricted(false);
        userJpaRepository.save(user);
    }

    @Transactional
    public void lockUser(String loginId) {
        Users user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (user.isRestricted()) {
            throw new IllegalStateException("이미 잠금 처리된 계정입니다.");
        }

        user.setRestricted(true);
        userJpaRepository.save(user);
    }

    @Transactional
    public List<InquiryResponseDto> getAllInquiries() {
       return inquiryJpaRepository.findAll()
               .stream()
               .map(inquiry -> {
                   Users user = inquiry.getUser();
                   return InquiryResponseDto.builder()
                           .id(inquiry.getId())
                           .userId(inquiry.getUser().getId())
                           .userName(inquiry.getUser().getName())
                           .title(inquiry.getTitle())
                           .category(inquiry.getCategory())
                           .status(inquiry.getStatus())
                           .createdAt(inquiry.getCreatedAt())
                           .content(inquiry.getContent())
                           .build();
               })
               .collect(Collectors.toList());
    }

    public InquiryAnswerResponseDto writeInquiryAnswer(Long adminId, InquiryAnswerRequestDto dto) {
        Inquiry inquiry = inquiryJpaRepository.findById(dto.getInquiryId())
                .orElseThrow(() -> new InquiryNotFoundException(ErrorCode.INQUIRY_NOT_FOUND));

        if(inquiryAnswerJpaRepository.existsByInquiry(inquiry)) {
            throw new RuntimeException("이미 답변이 작성된 문의입니다.");
        }

        InquiryAnswer answer = InquiryAnswer.builder()
                .inquiry(inquiry)
                .content(dto.getContent())
                .adminId(adminId)
                .build();

        inquiryAnswerJpaRepository.save(answer);

        inquiry.setStatus(InquiryStatus.ANSWERED);
        inquiryJpaRepository.save(inquiry);

        return InquiryAnswerResponseDto.builder()
                .inquiryId(inquiry.getId())
                .content(answer.getContent())
                .adminId(adminId)
                .createdAt(answer.getCreatedAt())
                .build();
    }

    public List<InquiryResponseDto> getNotAnswerInquiries(InquiryStatus status) {
        return inquiryJpaRepository.findByStatus(status)
                .stream()
                .map(inquiry -> {
                    Users user = inquiry.getUser();
                    return InquiryResponseDto.builder()
                            .id(inquiry.getId())
                            .userId(inquiry.getUser().getId())
                            .userName(inquiry.getUser().getName())
                            .title(inquiry.getTitle())
                            .category(inquiry.getCategory())
                            .status(inquiry.getStatus())
                            .createdAt(inquiry.getCreatedAt())
                            .content(inquiry.getContent())
                            .build();
                })
                .collect(Collectors.toList());
    }

    //Todo: 해외송금 리스트 출력, 승인/거절 처리

    //Todo: 수수료(송금/환전) 설정

}
