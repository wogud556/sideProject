package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.RepaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentAllocationRepository extends JpaRepository<RepaymentAllocation, Long> {

    List<RepaymentAllocation> findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(Long transactionId);
}
