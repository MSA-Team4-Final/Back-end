package com.project.korex.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MyInfoUpdateRequestDto {

    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "휴대폰 번호", example = "01012345678", required = true)
    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(
            regexp = "^(010|011|016|017|018|019)\\d{7,8}$",
            message = "올바른 휴대폰 번호 형식이 아닙니다."
    )
    private String phone;

    @Schema(description = "생년월일", example = "20000101", required = true)
    @NotBlank(message = "생년월일을 입력해주세요.")
    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 8자리 숫자(yyyyMMdd) 형식이어야 합니다.")
    private String birth;
}
