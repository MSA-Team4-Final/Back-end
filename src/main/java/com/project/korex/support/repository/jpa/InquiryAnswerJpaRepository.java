package com.project.korex.support.repository.jpa;

import com.project.korex.support.entity.Inquiry;
import com.project.korex.support.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryAnswerJpaRepository extends JpaRepository<InquiryAnswer, Long> {
    boolean existsByInquiry(Inquiry inquiry);
    boolean existsByInquiryId(Long inquiryId);
    Optional<InquiryAnswer> findByInquiryId(Long inquiryId);
}
