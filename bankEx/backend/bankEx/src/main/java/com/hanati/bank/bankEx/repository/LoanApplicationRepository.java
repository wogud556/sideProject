package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByUserId(String userId);
}
