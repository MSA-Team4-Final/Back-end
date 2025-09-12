package com.project.korex.ForeignTransfer.repository;

import com.project.korex.ForeignTransfer.entity.ForeignTransferTransaction;
import com.project.korex.user.entity.Users;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForeignTransferTransactionRepository extends JpaRepository<ForeignTransferTransaction, Long> {

    List<ForeignTransferTransaction> findAllByUser_LoginId(String loginId);

    List<ForeignTransferTransaction> findAllByUser_LoginIdAndCreatedAtBetween(
            String loginId, LocalDateTime start, LocalDateTime end);
}

