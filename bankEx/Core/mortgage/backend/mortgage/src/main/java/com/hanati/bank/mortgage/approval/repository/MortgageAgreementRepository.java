package com.hanati.bank.mortgage.approval.repository;

import com.hanati.bank.mortgage.approval.entity.MortgageAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgageAgreementRepository extends JpaRepository<MortgageAgreement, Long> {
    Optional<MortgageAgreement> findByApplicationId(Long applicationId);
}
