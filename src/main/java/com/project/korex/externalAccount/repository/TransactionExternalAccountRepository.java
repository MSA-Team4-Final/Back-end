package com.project.korex.externalAccount.repository;

import com.project.korex.externalAccount.entity.TransactionExternalAccount;
import com.project.korex.externalAccount.enums.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionExternalAccountRepository extends JpaRepository<TransactionExternalAccount, Long> {
    /**
     * 거래 ID 목록으로 외부계좌 매핑 정보 조회 (외부계좌 정보 포함)
     */
    @Query("SELECT tea FROM TransactionExternalAccount tea " +
            "LEFT JOIN FETCH tea.externalAccount ea " +
            "LEFT JOIN FETCH ea.user " +
            "WHERE tea.transaction.id IN :transactionIds")
    List<TransactionExternalAccount> findByTransactionIdInWithExternalAccount(@Param("transactionIds") List<Long> transactionIds);

    /**
     * 특정 거래의 특정 역할 외부계좌 조회
     */
    @Query("SELECT tea FROM TransactionExternalAccount tea " +
            "LEFT JOIN FETCH tea.externalAccount " +
            "WHERE tea.transaction.id = :transactionId AND tea.accountRole = :role")
    Optional<TransactionExternalAccount> findByTransactionIdAndRole(@Param("transactionId") Long transactionId,
                                                                    @Param("role") AccountRole role);

    /**
     * 거래별 모든 외부계좌 매핑 조회
     */
    @Query("SELECT tea FROM TransactionExternalAccount tea " +
            "LEFT JOIN FETCH tea.externalAccount " +
            "WHERE tea.transaction.id = :transactionId")
    List<TransactionExternalAccount> findByTransactionIdWithExternalAccount(@Param("transactionId") Long transactionId);

}


