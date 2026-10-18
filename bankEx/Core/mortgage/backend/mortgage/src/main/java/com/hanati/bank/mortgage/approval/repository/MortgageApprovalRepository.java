package com.hanati.bank.mortgage.approval.repository;

import com.hanati.bank.mortgage.approval.entity.MortgageApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgageApprovalRepository extends JpaRepository<MortgageApproval, Long> {
    Optional<MortgageApproval> findByApplicationId(Long applicationId);
}
