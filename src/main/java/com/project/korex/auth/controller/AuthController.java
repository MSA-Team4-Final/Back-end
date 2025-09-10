package com.project.korex.auth.controller;

import com.project.korex.auth.dto.request.*;
import com.project.korex.auth.dto.response.AuthStatusDto;
import com.project.korex.auth.dto.response.FindIdResponse;
import com.project.korex.auth.dto.response.ResetPasswordResponse;
import com.project.korex.auth.dto.response.UserInfoDto;
import com.project.korex.auth.exception.TokenNotFoundException;
import com.project.korex.auth.service.AuthService;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.security.jwt.JwtProvider;
import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.user.enums.VerificationPurpose;
import com.project.korex.user.repository.jpa.EmailVerificationTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.project.korex.common.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Auth API")
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationTokenRepository tokenRepository;
    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthStatusDto> loginMember(@RequestBody @Valid LoginRequestDto loginRequestDto, HttpServletResponse response) {
        Map<String, Object> authData = authService.login(loginRequestDto);

        String accessToken = (String) authData.get("accessToken");
        String refreshToken = (String) authData.get("refreshToken");
        UserInfoDto userInfo = (UserInfoDto) authData.get("userInfo");

        response.addHeader("Authorization", "Bearer " + accessToken);
        cookieUtil.addCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 14);

        AuthStatusDto authStatusDto = new AuthStatusDto(true, userInfo, accessToken);
        return new ResponseEntity<>(authStatusDto, HttpStatus.OK);
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@Valid @RequestBody SendCodeRequest req)
            throws jakarta.mail.MessagingException, UnsupportedEncodingException {
        authService.sendVerificationCode(req.getEmail(), VerificationPurpose.valueOf(req.getPurpose()));
        return ResponseEntity.ok(Map.of("success", true, "message", "인증 코드가 전송되었습니다."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest req) {
        authService.verifyCode(req.getEmail(), req.getCode(), VerificationPurpose.valueOf(req.getPurpose()));
        return ResponseEntity.ok(Map.of("success", true, "message", "이메일 인증이 완료되었습니다."));
    }

    @Operation(summary = "회원가입", description = "회원가입을 진행합니다.")
    @PostMapping("/join")
    public ResponseEntity<String> joinMember(@RequestBody @Valid JoinRequestDto joinRequestDto) throws MessagingException {
        authService.joinMember(joinRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponse> findId(@Valid @RequestBody FindIdRequest req) {
        String loginId = authService.findId(req);
        return ResponseEntity.ok(new FindIdResponse(loginId));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPasswordAfterVerify(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(new ResetPasswordResponse("비밀번호가 성공적으로 변경되었습니다."));
    }

    // 상태 확인 엔드포인트
    @Operation(summary = "인증 상태 확인", description = "현재 로그인된 사용자의 인증 상태를 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<AuthStatusDto> getAuthStatus(@AuthenticationPrincipal CustomUserPrincipal userDetails) {
        if (userDetails != null) {
            String role = null;
            var auths = userDetails.getAuthorities();
            if (auths != null && !auths.isEmpty()) {
                role = auths.iterator().next().getAuthority();
            }
            // 이메일 인증 여부
            boolean emailVerified = tokenRepository.existsByEmailAndVerifiedTrue(userDetails.getUser().getEmail());

            System.out.print("emailVerified: " + emailVerified);
            String status = userDetails.getUser().isRestricted() ? "RESTRICTED" : "UNLOCKED";
            if (emailVerified && !userDetails.getUser().isRestricted()) {
                status = "VERIFIED";
            }

            UserInfoDto userInfo = new UserInfoDto(
                    userDetails.getUser().getId(),
                    userDetails.getName(),
                    role,
                    emailVerified,
                    status

            );

            List<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            String accessToken = jwtProvider.createAccessToken(userDetails.getUser().getLoginId(), authorities);
            return ResponseEntity.ok(new AuthStatusDto(true, userInfo, accessToken));
        } else {
            return ResponseEntity.ok(new AuthStatusDto(false, null, null));
        }
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰 쿠키를 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.getCookie(request, "refreshToken").ifPresent(cookie -> {
            authService.logout(cookie.getValue());
        });

        // 쿠키의 유효기간을 0으로 설정
        cookieUtil.deleteCookie(response, "refreshToken");

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 통해 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/token/reissue")
    public ResponseEntity<Map<String, Object>> reissue(HttpServletRequest request, HttpServletResponse response) {
        log.info("/token reissue = {}", request.getRequestURI());
        // 쿠키에서 리프레시 토큰을 가져옴
        String refreshToken = cookieUtil.getCookie(request, "refreshToken")
                .map(Cookie::getValue)
                .orElseThrow(() -> new TokenNotFoundException(ErrorCode.TOKEN_NOT_FOUND));

        // 서비스를 호출하여 새로운 액세스 토큰을 발급
        HashMap<String, String> tokenMap = authService.reissue(refreshToken);

        // 응답 헤더에 새로운 액세스 토큰을 추가
        response.addHeader("Authorization", "Bearer " + tokenMap.get("accessToken"));
        // 쿠키에 새로운 리프레시 토큰 추가
        cookieUtil.addCookie(response, "refreshToken", tokenMap.get("refreshToken"), 60 * 60 * 24 * 14);

        return ResponseEntity.ok(Map.of(
                "accessToken", tokenMap.get("accessToken"),
                "tokenType", "Bearer"
        ));
    }


}
