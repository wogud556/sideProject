package com.hanati.bank.mortgage.application.repository;

import com.hanati.bank.mortgage.application.entity.MortgageDebt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MortgageDebtRepository extends JpaRepository<MortgageDebt, Long> {
    List<MortgageDebt> findByApplicationId(Long applicationId);
}
