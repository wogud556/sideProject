package com.hanati.bank.mortgage.collateral.repository;

import com.hanati.bank.mortgage.collateral.entity.MortgageValuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MortgageValuationRepository extends JpaRepository<MortgageValuation, Long> {

    List<MortgageValuation> findByPropertyIdOrderByValuationIdDesc(Long propertyId);

    Optional<MortgageValuation> findFirstByPropertyIdOrderByValuationIdDesc(Long propertyId);
}
