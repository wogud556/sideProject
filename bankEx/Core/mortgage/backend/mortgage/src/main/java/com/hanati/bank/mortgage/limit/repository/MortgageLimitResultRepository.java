package com.hanati.bank.mortgage.limit.repository;

import com.hanati.bank.mortgage.limit.entity.MortgageLimitResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgageLimitResultRepository extends JpaRepository<MortgageLimitResult, Long> {
    Optional<MortgageLimitResult> findFirstByApplicationIdOrderByLimitResultIdDesc(Long applicationId);
}
