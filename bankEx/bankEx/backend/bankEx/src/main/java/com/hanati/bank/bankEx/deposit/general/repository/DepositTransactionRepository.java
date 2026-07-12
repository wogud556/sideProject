package com.hanati.bank.bankEx.deposit.general.repository;

import com.hanati.bank.bankEx.deposit.general.entity.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {
    List<DepositTransaction> findByAccountNumberOrderByTransactionAtDesc(String accountNumber);
}
