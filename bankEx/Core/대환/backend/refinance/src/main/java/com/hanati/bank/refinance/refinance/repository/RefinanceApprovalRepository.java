package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.entity.RefinanceApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefinanceApprovalRepository extends JpaRepository<RefinanceApproval, Long> {
    Optional<RefinanceApproval> findByApplicationId(Long applicationId);
}
