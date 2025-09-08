package com.project.korex.support.dto;

import com.project.korex.common.BaseEntity;
import com.project.korex.support.entity.Inquiry;
import com.project.korex.support.enums.InquiryCategory;
import com.project.korex.support.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponse {

    private Long id;
    private String title;
    private InquiryCategory category;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private String content;

    public static InquiryResponse from(Inquiry inq) {
        return InquiryResponse.builder()
                .id(inq.getId())
                .title(inq.getTitle())
                .category(inq.getCategory())
                .status(inq.getStatus())
                .createdAt(inq.getCreatedAt())
                .content(inq.getContent())
                .build();
    }
}
