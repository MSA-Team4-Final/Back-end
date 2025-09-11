package com.project.korex.externalAccount.repository;

import com.project.korex.externalAccount.entity.ExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.korex.externalAccount.entity.Bank;
import com.project.korex.user.entity.Users;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {

    // 사용자의 활성화된 계좌 목록 조회 (주계좌 먼저, 생성일자 순)
    List<ExternalAccount> findByUserAndDeletedFalseOrderByIsPrimaryDescCreatedAtAsc(Users user);

    // 사용자의 주계좌 조회
    Optional<ExternalAccount> findByUserAndIsPrimaryTrue(Users user);

    // 중복 계좌 체크
    boolean existsByUserAndBankCodeAndAccountNumberAndDeletedFalse(
            Users user, Bank bank, String accountNumber);

    // 사용자의 활성화된 계좌 수 카운트
    long countByUserAndDeletedFalse(Users user);

    // 사용자의 모든 계좌를 주계좌 해제
    @Modifying
    @Query("UPDATE ExternalAccount ea SET ea.isPrimary = false WHERE ea.user = :user")
    void updateAllToPrimaryFalseAndDeletedFalse(@Param("user") Users user);

    // 특정 계좌 조회
    Optional<ExternalAccount> findByIdAndUser(
            Long externalAccountId, Users user);
}

