package com.hanati.bank.bankEx.deposit.general.repository;

import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountInfoRepository extends JpaRepository<AccountInfo, Long> {
    List<AccountInfo> findByUserId(String userId);

    Optional<AccountInfo> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountInfo a where a.accountNumber = :accountNumber")
    Optional<AccountInfo> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
