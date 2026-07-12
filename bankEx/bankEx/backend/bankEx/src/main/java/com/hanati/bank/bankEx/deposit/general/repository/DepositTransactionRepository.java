package com.hanati.bank.bankEx.deposit.general.repository;

import com.hanati.bank.bankEx.deposit.general.entity.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {
    List<DepositTransaction> findByAccountNumberOrderByTransactionAtDesc(String accountNumber);

    boolean existsByTransactionNumber(String transactionNumber);

    @Query("select coalesce(sum(t.amount), 0) from DepositTransaction t " +
           "where t.accountNumber = :accountNumber and t.transactionType = 'TRANSFER_WITHDRAWAL' " +
           "and t.transactionAt >= :start and t.transactionAt < :end")
    Long sumTransferWithdrawalAmount(@Param("accountNumber") String accountNumber,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
