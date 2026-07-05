package com.hanati.bank.bankEx.repository;

import com.hanati.bank.bankEx.entity.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {
    List<DepositTransaction> findByAccountNumberOrderByTransactionAtDesc(String accountNumber);
}
