package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.entity.RefinanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefinanceReviewRepository extends JpaRepository<RefinanceReview, Long> {
    Optional<RefinanceReview> findByApplicationId(Long applicationId);
}
