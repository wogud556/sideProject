package com.hanati.bank.mortgage.application.repository;

import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgagePropertyRepository extends JpaRepository<MortgageProperty, Long> {
    Optional<MortgageProperty> findByApplicationId(Long applicationId);
}
