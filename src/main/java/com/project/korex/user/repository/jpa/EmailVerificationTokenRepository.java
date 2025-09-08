package com.project.korex.user.repository.jpa;

import com.project.korex.user.entity.EmailVerificationToken;
import com.project.korex.user.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByCode(String code);
    Optional<EmailVerificationToken> findTopByEmailOrderByExpiryDateDesc(String email);
    Optional<EmailVerificationToken> findTopByEmailAndPurposeOrderByExpiryDateDesc(String email, VerificationPurpose purpose);

    boolean existsByEmailAndVerifiedTrue(String email);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email")
    int deleteAllByEmail(@Param("email") String email);
}
