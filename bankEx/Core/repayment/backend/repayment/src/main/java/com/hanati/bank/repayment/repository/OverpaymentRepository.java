package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.Overpayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OverpaymentRepository extends JpaRepository<Overpayment, Long> {

    List<Overpayment> findByLoanAccountIdOrderByIdDesc(Long loanAccountId);

    List<Overpayment> findByRepaymentTransactionId(Long repaymentTransactionId);
}
