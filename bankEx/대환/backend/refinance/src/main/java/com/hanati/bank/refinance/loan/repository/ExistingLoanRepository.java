package com.hanati.bank.refinance.loan.repository;

import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExistingLoanRepository extends JpaRepository<ExistingLoan, Long> {
    List<ExistingLoan> findByCustomerId(Long customerId);
}
