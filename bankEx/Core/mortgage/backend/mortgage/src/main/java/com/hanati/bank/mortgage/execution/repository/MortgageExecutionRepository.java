package com.hanati.bank.mortgage.execution.repository;

import com.hanati.bank.mortgage.execution.entity.MortgageExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MortgageExecutionRepository extends JpaRepository<MortgageExecution, Long> {

    Optional<MortgageExecution> findByIdempotencyKey(String idempotencyKey);

    List<MortgageExecution> findByApplicationIdOrderByExecutionIdDesc(Long applicationId);
}
