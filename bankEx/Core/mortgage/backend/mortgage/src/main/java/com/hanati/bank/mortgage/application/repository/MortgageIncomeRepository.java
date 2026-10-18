package com.hanati.bank.mortgage.application.repository;

import com.hanati.bank.mortgage.application.entity.MortgageIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgageIncomeRepository extends JpaRepository<MortgageIncome, Long> {
    Optional<MortgageIncome> findByApplicationId(Long applicationId);
}
