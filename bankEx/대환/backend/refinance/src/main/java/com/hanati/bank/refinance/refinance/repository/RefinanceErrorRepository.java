package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.entity.RefinanceError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefinanceErrorRepository extends JpaRepository<RefinanceError, Long> {
    Optional<RefinanceError> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
