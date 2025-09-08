package com.project.korex.user.service;

import com.project.korex.auth.exception.DuplicateEmailException;
import com.project.korex.auth.exception.DuplicatePhoneException;
import com.project.korex.auth.exception.LoginFailedException;
import com.project.korex.auth.exception.PasswordMismatchException;
import com.project.korex.auth.service.AuthService;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.transaction.exception.CannotTransferToSelfException;
import com.project.korex.user.dto.ChangePasswordRequestDto;
import com.project.korex.user.dto.MyInfoResponseDto;
import com.project.korex.user.dto.MyInfoUpdateRequestDto;
import com.project.korex.user.dto.VerifyRecipientRequestDto;
import com.project.korex.user.entity.Users;
import com.project.korex.user.enums.VerificationPurpose;
import com.project.korex.user.repository.jpa.EmailVerificationTokenRepository;
import com.project.korex.user.repository.jpa.RefreshTokenRepository;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public MyInfoResponseDto getProfile(String loginId) {
        Users user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        String email = user.getEmail();
        boolean emailVerified = false;

        if (email != null && !email.isBlank()) {
            emailVerified = emailVerificationTokenRepository
                    .existsByEmailAndVerifiedTrue(email);
        }

        return MyInfoResponseDto.builder()
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birth(user.getBirth())
                .emailVerified(emailVerified)
                .roleName(user.getRole().getRoleName())
                .build();
    }

    @Transactional
    public void updateUserInfo(String loginId, MyInfoUpdateRequestDto req) {
        Users user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 이메일 변경
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userJpaRepository.existsByEmailAndIdNot(newEmail, user.getId())) {
                    throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL);
                }
                String oldEmail = user.getEmail();
                if (oldEmail != null && !oldEmail.isBlank()) {
                    emailVerificationTokenRepository.deleteAllByEmail(oldEmail);
                }
                user.setEmail(newEmail);
            }
        }

        // 휴대폰 변경
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String normalized = req.getPhone().replaceAll("\\D", "");
            if (!normalized.equals(user.getPhone())) {
                if (userJpaRepository.existsByPhoneAndIdNot(normalized, user.getId())) {
                    throw new DuplicatePhoneException(ErrorCode.DUPLICATE_PHONE);
                }
                user.setPhone(normalized);
            }
        }

        // 생년월일 변경
        if (req.getBirth() != null && !req.getBirth().equals(user.getBirth())) {
            user.setBirth(req.getBirth());
        }

        log.info("회원정보 수정 완료 ID: {}, loginId: {}", user.getId(), user.getLoginId());
    }

    @Transactional
    public void changePassword(String loginId, ChangePasswordRequestDto req) {
        Users user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 현재 비밀번호 일치 확인
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new LoginFailedException(ErrorCode.PASSWORD_MISMATCH, user.getFailCount(), user.isRestricted());
        }

        // 새 비밀번호 확인 일치
        if (!req.getNewPassword().equals(req.getNewPasswordCheck())) {
            throw new PasswordMismatchException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 이전과 동일 비밀번호 금지
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userJpaRepository.save(user);

        // 세션/토큰 무효화
        refreshTokenRepository.deleteByUser(user);
    }

    // 이름 존재 여부 확인
    public boolean existsByName(String name) {
        try {
            return userJpaRepository.existsByName(name);
        } catch (Exception e) {
            log.error("이름 존재 확인 실패: {}", e.getMessage());
            throw new RuntimeException("사용자 조회 중 오류가 발생했습니다");
        }
    }

    // 이름 + 전화번호 매칭 확인
    public boolean verifyRecipient(VerifyRecipientRequestDto dto) {
        try {
            Users user = userJpaRepository.findByPhone(dto.getPhone())
                    .orElseThrow(() -> new RuntimeException("해당 번호의 사용자를 찾을 수 없습니다"));

            if(user.getId().equals(dto.getCurrentUserId())){
                throw new CannotTransferToSelfException(ErrorCode.TRANSFER_NOT_SELF);
            }

            return user.getName().equals(dto.getName()) ;
        } catch (Exception e) {
            log.error("수취인 검증 실패: {}", e.getMessage());
            throw new RuntimeException("수취인 검증 중 오류가 발생했습니다");
        }
    }
}
