package com.project.korex.support.dto;

import com.project.korex.support.entity.InquiryAnswer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryAnswerResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    public static InquiryAnswerResponse from(InquiryAnswer ans) {
        return InquiryAnswerResponse.builder()
                .id(ans.getId())
                .content(ans.getContent())
                .createdAt(ans.getCreatedAt())
                .build();
    }
}
