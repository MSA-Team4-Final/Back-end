package com.project.korex.user.entity;

import com.project.korex.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_restricted", columnList = "restricted")
        }
)
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 인조키

    @Column(name = "login_id", unique = true, nullable = false, length = 50)
    private String loginId; // 실제 로그인 ID

    @Column(nullable=false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(unique = true, nullable = false, length = 100)
    private String phone;

    @Column(nullable = false, length = 30)
    private String birth;

    @Column(name = "krw_account", nullable = false, length = 15)
    private String krwAccount;

    @Column(name = "foreign_account", nullable = false, length = 15)
    private String foreignAccount;

    @Column(name = "transaction_password")
    private String transactionPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role; // 회원 권한 (USER, ADMIN)

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Column(name="fail_count", nullable=false)
    private int failCount = 0;  // 로그인 실패 횟수

    @Column(name="restricted", nullable=false)
    private boolean restricted = false;

    @Column(name="restricted_at")
    private LocalDateTime restrictedAt;

    @Builder
    private Users(String loginId, String password, String name, String email, String phone, String birth, String krwAccount, String foreignAccount, String transactionPassword, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birth = birth;
        this.krwAccount = krwAccount;
        this.foreignAccount = foreignAccount;
        this.transactionPassword = transactionPassword;
        this.role = role;
    }

    public void addRefreshToken(RefreshToken refreshToken) {
        this.refreshTokens.add(refreshToken);
        refreshToken.setUser(this);
    }

    public void onPasswordFail(int threshold) {
        this.failCount++;
        if (this.failCount >= threshold && !this.restricted) {
            this.restricted = true;
            this.restrictedAt = LocalDateTime.now();
        }
    }

    public void resetFailCount() {this.failCount = 0;}
    public boolean isAdmin() {
        return role != null && "ROLE_ADMIN".equalsIgnoreCase(role.getRoleName());
    }
}
