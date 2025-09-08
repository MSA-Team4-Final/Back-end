package com.project.korex.auth.service;

import com.project.korex.account.exception.InvalidAccountPasswordException;
import com.project.korex.account.service.AccountPasswordService;
import com.project.korex.auth.dto.request.FindIdRequest;
import com.project.korex.auth.dto.request.JoinRequestDto;
import com.project.korex.auth.dto.request.LoginRequestDto;
import com.project.korex.auth.dto.response.UserInfoDto;
import com.project.korex.auth.exception.*;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.common.security.jwt.JwtProvider;
import com.project.korex.transaction.enums.AccountType;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.user.entity.EmailVerificationToken;
import com.project.korex.user.entity.RefreshToken;
import com.project.korex.user.entity.Role;
import com.project.korex.user.entity.Users;
import com.project.korex.user.enums.RoleType;
import com.project.korex.user.enums.VerificationPurpose;
import com.project.korex.user.repository.jpa.EmailVerificationTokenRepository;
import com.project.korex.user.repository.jpa.RefreshTokenRepository;
import com.project.korex.user.repository.jpa.RoleJpaRepository;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final CurrencyRepository currencyRepository;
    private final BalanceRepository balanceRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final SecureRandom random = new SecureRandom();
    private final AccountPasswordService passwordService;

    private static final int VERIFY_EXPIRE_MINUTES = 10;

    @Value("${custom.site-host}")
    private String siteHost;

    public void sendVerificationCode(String email, VerificationPurpose purpose)
            throws MessagingException, UnsupportedEncodingException {
        if (userJpaRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL);
        }

        String code = generateRandomCode();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .email(email)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .purpose(purpose)
                .build();

        tokenRepository.save(token);
        sendEmail(email, code, purpose);
    }

    public void verifyCode(String email, String inputCode, VerificationPurpose purpose) {
        var token = tokenRepository
                .findTopByEmailAndPurposeOrderByExpiryDateDesc(email, purpose)
                .orElseThrow(() -> new VerificationTokenNotFoundException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));

        if (token.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new TokenExpriedException(ErrorCode.TOKEN_EXPIRED);

        if (!token.getCode().equals(inputCode))
            throw new InvalidVerificationCodeException(ErrorCode.INVALID_CODE);

        token.markAsVerified();
        tokenRepository.save(token);
    }

    public void assertVerified(String email, VerificationPurpose purpose) {
        EmailVerificationToken latest = tokenRepository
                .findTopByEmailAndPurposeOrderByExpiryDateDesc(email, purpose)
                .orElseThrow(() -> new TokenNotFoundException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!latest.isVerified() || latest.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new EmailNotVerifiedException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
    }

    private String generateRandomCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private void sendEmail(String to, String code, VerificationPurpose purpose)
            throws MessagingException, UnsupportedEncodingException {
        Context ctx = new Context(Locale.KOREA);
        ctx.setVariable("brand", "Korex");
        ctx.setVariable("recipientName", "고객님");
        ctx.setVariable("minutes", VERIFY_EXPIRE_MINUTES);
        ctx.setVariable("code", code);
        ctx.setVariable("supportEmail", "support@korex.com");

        String subject = (purpose == VerificationPurpose.SIGN_UP)
                ? "[Korex] 이메일 인증 코드"
                : "[Korex] 비밀번호 재설정 인증 코드";
        String template = "email-verification";

        String html = templateEngine.process(template, ctx);

        // 메시지 생성/전송
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true); // HTML
        helper.setFrom(new InternetAddress("ghyunjin0913@gmail.com", "Korex")); // 발신자명

        mailSender.send(message);
    }

    public void joinMember(JoinRequestDto joinRequestDto) {
        // 비밀번호 확인 검증
        if (!joinRequestDto.getPassword().equals(joinRequestDto.getPasswordCheck())) {
            throw new PasswordMismatchException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 계좌 비밀번화 확인 검증
        if (!joinRequestDto.getTransactionPassword().equals(joinRequestDto.getTransactionPasswordCheck())) {
            throw new PasswordMismatchException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 아이디 중복 검증
        String loginId = joinRequestDto.getLoginId();
        if (userJpaRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 이메일 중복 검증
        String email = joinRequestDto.getEmail();
        if (userJpaRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 휴대폰 번호 중복 검증
        String phone = joinRequestDto.getPhone();
        if (userJpaRepository.existsByPhone(phone)) {
            throw new DuplicatePhoneException(ErrorCode.DUPLICATE_PHONE);
        }

        // 가입 검증 시
        assertVerified(email, VerificationPurpose.SIGN_UP);

        // 계좌 비밀번호 규칙 검증
        if (!passwordService.validatePassword(joinRequestDto.getTransactionPassword(),
                joinRequestDto.getBirth(),
                joinRequestDto.getPhone())) {
            throw new InvalidAccountPasswordException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }

        // 유저 타입 가져오기
        Role role = roleJpaRepository.findByRoleName(RoleType.USER.getKey())
                .orElseThrow(() -> new RoleNotFoundException(ErrorCode.ROLE_NOT_FOUND));

        String krwAccount = generateAccountNumber("KRW");
        String foreignAccount = generateAccountNumber("FOREIGN");

        String encryptedAccountPassword = passwordService.encryptPassword(joinRequestDto.getTransactionPassword());

        // 유저 빌더 엔티티 생성(암호화 포함)
        Users newUser = Users.builder()
                .loginId(joinRequestDto.getLoginId())
                .password(passwordEncoder.encode(joinRequestDto.getPassword()))
                .name(joinRequestDto.getName())
                .email(joinRequestDto.getEmail())
                .phone(joinRequestDto.getPhone())
                .birth(joinRequestDto.getBirth())
                .krwAccount(krwAccount)
                .foreignAccount(foreignAccount)
                .transactionPassword(encryptedAccountPassword)
                .role(role)
                .build();

        // 유저 저장
        Users savedUser = userJpaRepository.save(newUser);
        createAccountBalances(savedUser);
        log.info("회원가입 성공 ID : {}, loginId : {}", newUser.getId(), newUser.getLoginId());
    }

    @Transactional(noRollbackFor = LoginFailedException.class)
    private String generateAccountNumber(String accountType) {
        Random random = new Random();

        // 첫 3자리를 계좌 타입별로 구분
        String bankCode;
        if ("KRW".equals(accountType)) {
            bankCode = "100";  // 원화계좌 코드
        } else {
            bankCode = "200";  // 외화계좌 코드
        }

        // 나머지 8자리는 동일하게 생성
        int secondPart = 100000 + random.nextInt(900000);  // 6자리
        int thirdPart = 10 + random.nextInt(90);           // 2자리

        return String.format("%s-%06d-%02d", bankCode, secondPart, thirdPart);
    }

    private void createAccountBalances(Users user) {
        List<Currency> allCurrencies = currencyRepository.findAll();
        List<Balance> balances = new ArrayList<>();

        for (Currency currency : allCurrencies) {
            if ("KRW".equals(currency.getCode())) {
                // KRW는 원화계좌에
                balances.add(createBalance(user, currency, AccountType.KRW));
            } else {
                // 다른 모든 통화는 외화계좌에
                balances.add(createBalance(user, currency, AccountType.FOREIGN));
            }
        }

        balanceRepository.saveAll(balances);
        log.info("사용자 {}에 대해 원화/외화 계좌의 {}개 통화 잔액이 생성되었습니다.",
                user.getName(), balances.size());
    }

    private Balance createBalance(Users user, Currency currency, AccountType accountType) {
        return Balance.builder()
                .user(user)
                .currency(currency)
                .accountType(accountType)
                .availableAmount(BigDecimal.ZERO)
                .heldAmount(BigDecimal.ZERO)
                .build();
    }

    @Transactional(noRollbackFor = LoginFailedException.class)
    public Map<String, Object> login(LoginRequestDto loginRequestDto) {
        String loginId = loginRequestDto.getLoginId();
        Users findUser = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), findUser.getPassword())) {
            int next = findUser.getFailCount() + 1;
            findUser.setFailCount(next);

            // 5회 이상 틀릴 경우 계정 제한
            if (next >= 5 && !findUser.isRestricted()) {
                findUser.setRestricted(true);
                findUser.setRestrictedAt(LocalDateTime.now());
                log.warn("비밀번호 {}회 연속 실패로 제한 전환: loginId={}", next, loginId);
            }
            userJpaRepository.save(findUser);
            throw new LoginFailedException(ErrorCode.PASSWORD_MISMATCH, next, findUser.isRestricted());
        }

        if (findUser.getFailCount() > 0) {
            findUser.setFailCount(0);
            userJpaRepository.save(findUser);
        }

        boolean emailVerified = false;
        String email = findUser.getEmail();
        if (email != null && !email.isBlank()) {
            emailVerified = tokenRepository
                    .existsByEmailAndPurposeAndVerifiedTrue(email, VerificationPurpose.SIGN_UP);
        }

        List<String> authorities = new ArrayList<>();
        authorities.add(findUser.getRole().getRoleName());
        if (emailVerified) authorities.add("VERIFIED");
        if (findUser.isRestricted()) authorities.add("RESTRICTED");

        // 액세스 토큰 생성
        String accessToken  = jwtProvider.createAccessToken(findUser.getLoginId(), authorities);

        // 리프레시 토큰 생성
        String refreshToken = jwtProvider.createRefreshToken(findUser.getLoginId(), authorities);

        // 리프레시 토큰의 만료 시간
        LocalDateTime refreshTokenExpireTime = LocalDateTime.now().plusSeconds(JwtProvider.REFRESH_TOKEN_EXPIRE_TIME / 1000);

        // 토큰 빌더 엔티티 생성
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(findUser)
                .token(refreshToken)
                .expiredAt(refreshTokenExpireTime)
                .build();

        // 토큰 저장
        refreshTokenRepository.save(newRefreshToken);

        // 사용자 정보 생성
        UserInfoDto userInfo = new UserInfoDto(
                findUser.getId(),
                findUser.getLoginId(),
                findUser.getRole().getRoleName(),
                emailVerified
        );

        Map<String, Object> authData = new HashMap<>();
        authData.put("accessToken", accessToken);
        authData.put("refreshToken", refreshToken);
        authData.put("userInfo", userInfo);

        log.info("로그인 성공 및 토큰 생성. loginId = {}", loginId);
        return authData;
    }

    @Transactional(readOnly = true)
    public String findId(FindIdRequest req) {
        var user = userJpaRepository.findByEmailAndName(req.getEmail(), req.getName())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        return user.getLoginId();
    }

    public void resetPasswordAfterVerify(String email, String code, String newPassword) {
        verifyCode(email, code, VerificationPurpose.RESET_PASSWORD);

        Users user = userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(newPassword));
        userJpaRepository.save(user);
    }

    public void logout(String refreshToken) {
        // 토큰 찾고 있으면 제거
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            log.info("리프레시 토큰을 DB에서 제거했습니다. user: {}", token.getUser().getLoginId());
        });
    }

    public HashMap<String, String> reissue(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_TOKEN);
        }

        // DB에 저장된 토큰인지, 그리고 만료되지 않았는지 확인
        RefreshToken findRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException(ErrorCode.TOKEN_NOT_FOUND));

        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.delete(findRefreshToken);

        // 토큰에서 사용자 정보 추출 및 검증
        String loginId = jwtProvider.getLoginId(refreshToken);
        //String role = jwtProvider.getRole(refreshToken);

        // 리프레시 토큰 엔티티 저장하기 위해 추출
        Users findUser = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 이메일 인증 여부
        boolean emailVerified = false;
        String email = findUser.getEmail();
        if (email != null && !email.isBlank()) {
            emailVerified = tokenRepository
                    .existsByEmailAndPurposeAndVerifiedTrue(email, VerificationPurpose.SIGN_UP);
        }

        String role = findUser.getRole().getRoleName();
        List<String> authorities = new ArrayList<>();
        authorities.add(role);
        if (emailVerified) authorities.add("VERIFIED");

        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtProvider.createAccessToken(loginId, authorities);
        // 새로운 리프레시 토큰 생성
        String newRefreshToken = jwtProvider.createRefreshToken(loginId, authorities);
        LocalDateTime refreshTokenExpireTime = LocalDateTime.now().plusSeconds(JwtProvider.REFRESH_TOKEN_EXPIRE_TIME / 1000);

        // 토큰 빌더 엔티티 생성
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .user(findUser)
                .token(newRefreshToken)
                .expiredAt(refreshTokenExpireTime)
                .build();
        // DB 저장
        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("액세스 토큰을 재발급했습니다. user: {}", loginId);
        log.info("리프레시 토큰을 재발급했습니다. user: {}", loginId);

        // 컨트롤러 리턴용 맵
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", newAccessToken);
        tokenMap.put("refreshToken", newRefreshToken);
        // 컨트롤러에 새로운 액세스, 리프레시 토큰 반환
        return tokenMap;
    }
}
