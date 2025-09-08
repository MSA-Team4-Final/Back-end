package com.project.korex.account.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountPasswordService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 계좌 비밀번호 유효성 검사
     * @param password 4자리 숫자 문자열
     * @param birth 생년월일 (YYYYMMDD)
     * @param phone 휴대폰 번호 (숫자만)
     * @return true: 유효 / false: 유효하지 않음
     */
    public boolean validatePassword(String password, String birth, String phone) {
        if (password == null || password.length() != 4) return false;

        // 숫자 아닌 경우
        if (!password.matches("\\d{4}")) return false;

        // 연속 숫자 검사 (0123, 1234, 9876 등)
        if (isSequential(password)) return false;

        // 반복 숫자 검사 (1111, 2222)
        if (isRepeated(password)) return false;

        // 생년월일 포함 여부
        if (birth != null && birth.length() >= 4) {
            String birthPart = birth.substring(birth.length() - 4); // MMDD 또는 YYYYMMDD 끝 4자리
            if (password.equals(birthPart)) return false;
        }

        // 휴대폰 번호 포함 여부
        if (phone != null && phone.length() >= 4) {
            String last4Phone = phone.substring(phone.length() - 4);
            if (password.equals(last4Phone)) return false;
        }

        return true;
    }

    private boolean isSequential(String password) {
        for (int i = 0; i < password.length() - 1; i++) {
            if (password.charAt(i+1) - password.charAt(i) != 1) {
                return false;
            }
        }
        return true;
    }

    private boolean isRepeated(String pw) {
        char first = pw.charAt(0);
        for (int i = 1; i < pw.length(); i++) {
            if (pw.charAt(i) != first) return false;
        }
        return true;
    }

    /**
     * 비밀번호 암호화
     * @param password 원문 비밀번호
     * @return 암호화된 문자열
     */
    public String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 암호화 비밀번호 검증
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
