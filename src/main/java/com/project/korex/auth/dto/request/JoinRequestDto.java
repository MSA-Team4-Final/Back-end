package com.project.korex.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "JoinRequest", description = "회원 가입 요청 DTO")
public class JoinRequestDto {

    @Schema(description = "로그인 ID", example = "user123", required = true)
    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문과 숫자로 4~20자 이내로 입력해주세요.")
    private String loginId;

    @Schema(description = "비밀번호", example = "test123!", required = true)
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8~16자 이내로 입력해주세요."
    )
    private String password;

    // 비밀번호 확인을 위한 필드
    @Schema(description = "비밀번호 확인", example = "Passw0rd!", required = true)
    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordCheck;

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름을 입력해주세요.")
    private String name; // 실명

    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "이메일 인증 코드", example = "123456", required = true)
    @NotBlank(message = "이메일 인증 코드를 입력해주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String emailCode;

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

    private String transactionPassword;

    private String transactionPasswordCheck;

}
