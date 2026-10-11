package com.hanati.bank.screening.repository;

import com.hanati.bank.screening.entity.LoanScreening;
import com.hanati.bank.screening.enums.ScreeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanScreeningRepository extends JpaRepository<LoanScreening, Long> {
    List<LoanScreening> findAllByOrderByScreeningIdDesc();
    List<LoanScreening> findByStatusOrderByScreeningIdAsc(ScreeningStatus status);
}
