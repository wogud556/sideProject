package com.hanati.bank.refinance.refinance.repository;

import com.hanati.bank.refinance.refinance.domain.TransactionType;
import com.hanati.bank.refinance.refinance.entity.RefinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefinanceTransactionRepository extends JpaRepository<RefinanceTransaction, Long> {
    Optional<RefinanceTransaction> findByRequestId(String requestId);

    List<RefinanceTransaction> findByApplicationIdAndTransactionType(Long applicationId, TransactionType transactionType);

    List<RefinanceTransaction> findByApplicationIdOrderByStartedAtAsc(Long applicationId);
}
