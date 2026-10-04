package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.AccountingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountingEventRepository extends JpaRepository<AccountingEvent, Long> {

    List<AccountingEvent> findByRepaymentTransactionIdOrderByIdAsc(Long repaymentTransactionId);
}
