package com.project.korex.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class ChangePasswordRequestDto {
    @NotBlank
    private String currentPassword;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8~16자 이내로 입력해주세요."
    )
    private String newPassword;

    @NotBlank
    private String newPasswordCheck;

}

