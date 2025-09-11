package com.project.korex.externalAccount.repository;

import com.project.korex.externalAccount.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, Long> {

    // 활성화된 은행 목록 조회 (이름 순 정렬)
    List<Bank> findAllByOrderByBankName();

    // 특정 은행 코드로 활성화된 은행 조회
    Optional<Bank> findByBankCode(String bankCode);

    // 은행명으로 검색
    List<Bank> findByBankNameContainingOrderByBankName(String bankName);

    // 은행 코드 존재 여부 확인
    boolean existsByBankCode(String bankCode);

    @Query("SELECT DISTINCT b FROM Bank b " +
            "JOIN ExternalAccount ea ON ea.bankCode = b " +
            "WHERE ea.user.id = :userId AND ea.deleted = false " +
            "ORDER BY b.bankName")
    List<Bank> findBanksByUserActiveAccounts(@Param("userId") Long userId);
}
