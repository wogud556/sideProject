package com.hanati.bank.mortgage.screening.repository;

import com.hanati.bank.mortgage.screening.entity.MortgageScreening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MortgageScreeningRepository extends JpaRepository<MortgageScreening, Long> {

    List<MortgageScreening> findByApplicationIdOrderByScreeningIdDesc(Long applicationId);

    Optional<MortgageScreening> findFirstByApplicationIdOrderByScreeningIdDesc(Long applicationId);
}
