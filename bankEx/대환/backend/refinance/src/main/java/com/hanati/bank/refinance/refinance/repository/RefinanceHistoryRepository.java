package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefinanceHistoryRepository extends JpaRepository<RefinanceHistory, Long> {
    List<RefinanceHistory> findByApplicationIdOrderByProcessedAtAsc(Long applicationId);
}
