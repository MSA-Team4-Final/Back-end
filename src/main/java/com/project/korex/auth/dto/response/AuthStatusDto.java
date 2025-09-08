package com.project.korex.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Schema(name = "AuthStatus", description = "인증 상태 응답 DTO")
public class AuthStatusDto {

    @Schema(description = "인증 여부", example = "true", required = true)
    private boolean authenticated;
    @Schema(description = "사용자 정보", implementation = UserInfoDto.class, required = true)
    private UserInfoDto user;
    private String accessToken;

}
