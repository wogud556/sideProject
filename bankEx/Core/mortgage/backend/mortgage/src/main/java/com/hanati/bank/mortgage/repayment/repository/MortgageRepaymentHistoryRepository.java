package com.hanati.bank.mortgage.repayment.repository;

import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MortgageRepaymentHistoryRepository extends JpaRepository<MortgageRepaymentHistory, Long> {

    Optional<MortgageRepaymentHistory> findByIdempotencyKey(String idempotencyKey);

    List<MortgageRepaymentHistory> findByLoanAccountIdOrderByRepaymentIdDesc(Long loanAccountId);
}
