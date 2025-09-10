package com.project.korex.common.security.filter;

import com.project.korex.common.security.jwt.JwtProvider;
import com.project.korex.common.security.service.CustomUserDetailsService;
import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.common.util.CookieUtil;
import com.project.korex.user.enums.VerificationPurpose;
import com.project.korex.user.repository.jpa.EmailVerificationTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final CookieUtil cookieUtil;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, CustomUserDetailsService customUserDetailsService, CookieUtil cookieUtil, EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.jwtProvider = jwtProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.cookieUtil = cookieUtil;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                                                                                throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 인증이 필요 없는 경로는 필터 건너뜀
        if (uri.startsWith("/api/auth/send-code") || uri.startsWith("/api/auth/join")
                || uri.startsWith("/api/auth/verify-code")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 가져오고 없으면 null
        String jwtToken = request.getHeader("Authorization");
        String accessToken = null;

        if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
            accessToken = jwtToken.substring(7); // "Bearer " 제거
        }


        // validateToken으로 토큰 유효성 검사
        // 토큰이 존재하고, 유효하다면
        if (accessToken != null && jwtProvider.validateToken(accessToken)) {
            // 토큰에서 loginId를 추출
            String loginId = jwtProvider.getLoginId(accessToken);

            // loginId로 CustomUserDetailsService를 통해 DB에서 사용자 정보 조회
            CustomUserPrincipal userDetails = (CustomUserPrincipal) customUserDetailsService.loadUserByUsername(loginId);

            if (userDetails != null) {
                List<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());

                // 이메일 인증 여부
                boolean emailVerified = false;
                String email = userDetails.getEmail();
                if (email != null && !email.isBlank()) {
                    emailVerified = emailVerificationTokenRepository
                            .existsByEmailAndVerifiedTrue(email);
                }
                if (emailVerified && !userDetails.isRestricted()) {
                    authorities.add(new SimpleGrantedAuthority("VERIFIED"));
                }

                // RESTRICTED 계정은 로그인 허용하되 별도 권한 부여
                if (userDetails.isRestricted()) {
                    authorities.add(new SimpleGrantedAuthority("RESTRICTED"));
                    log.warn("제한된 계정 로그인: {}", loginId);
                } else {
                    authorities.add(new SimpleGrantedAuthority("UNLOCKED"));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //                Authentication authentication =
//                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // SecurityContext에 Authentication 객체를 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Security Context에 '{}' 인증 정보를 저장했습니다 (이메일 인증:{}, restricted:{}), uri: {}",
                        authentication.getName(), emailVerified, userDetails.isRestricted(), request.getRequestURI());
            }
        }

        // 다음 필터로 요청을 전달
        filterChain.doFilter(request, response);
    }
}
