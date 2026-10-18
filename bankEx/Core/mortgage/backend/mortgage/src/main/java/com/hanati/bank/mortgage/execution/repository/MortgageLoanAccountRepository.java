package com.hanati.bank.mortgage.execution.repository;

import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MortgageLoanAccountRepository extends JpaRepository<MortgageLoanAccount, Long> {

    Optional<MortgageLoanAccount> findByApplicationId(Long applicationId);

    List<MortgageLoanAccount> findByCustomerIdOrderByLoanAccountIdDesc(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MortgageLoanAccount a where a.loanAccountId = :id")
    Optional<MortgageLoanAccount> findByIdForUpdate(@Param("id") Long id);
}
