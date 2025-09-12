package com.project.korex.ForeignTransfer.repository;

import com.project.korex.ForeignTransfer.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    // 로그인 ID로 해당 사용자가 등록한 모든 수취인 정보 조회
    List<Recipient> findByUser_LoginId(String loginId);

    // 특정 수취인 조회 + 로그인 사용자 소유 확인
    Optional<Recipient> findByIdAndUser_LoginId(Long id, String loginId);

    // 활성화된 수취인 조회
    List<Recipient> findByUser_LoginIdAndIsActiveTrue(String loginId);

}