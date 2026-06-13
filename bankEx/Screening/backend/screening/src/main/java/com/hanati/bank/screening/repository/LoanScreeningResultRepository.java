package com.hanati.bank.screening.repository;

import com.hanati.bank.screening.entity.LoanScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanScreeningResultRepository extends JpaRepository<LoanScreeningResult, Long> {
    Optional<LoanScreeningResult> findByApplicationId(Long applicationId);
}
