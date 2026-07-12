package com.hanati.bank.bankEx.deposit.general.repository;

import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountInfoRepository extends JpaRepository<AccountInfo, Long> {
    List<AccountInfo> findByUserId(String userId);

    Optional<AccountInfo> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
