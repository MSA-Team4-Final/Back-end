package com.project.korex.ForeignTransfer.repository;

import com.project.korex.ForeignTransfer.entity.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
}
