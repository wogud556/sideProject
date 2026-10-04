package com.hanati.bank.bankEx.deposit.transfer.repository;

import com.hanati.bank.bankEx.deposit.transfer.entity.AccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTransferRepository extends JpaRepository<AccountTransfer, Long> {
    Optional<AccountTransfer> findByRequestId(String requestId);
}
