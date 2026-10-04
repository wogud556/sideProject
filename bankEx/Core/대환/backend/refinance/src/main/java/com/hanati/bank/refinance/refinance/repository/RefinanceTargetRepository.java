package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.entity.RefinanceTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefinanceTargetRepository extends JpaRepository<RefinanceTarget, Long> {
    List<RefinanceTarget> findByApplicationId(Long applicationId);
}
