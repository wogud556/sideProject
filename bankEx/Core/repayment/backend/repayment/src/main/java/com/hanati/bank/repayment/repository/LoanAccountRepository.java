package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.LoanAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {

    Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);

    /**
     * 계좌 단위 직렬 처리를 위한 비관적 쓰기 잠금 (명세 10번).
     * 동일 계좌에 동시에 들어온 상환/전액상환/취소 요청을 한 줄로 세운다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LoanAccount a WHERE a.id = :id")
    Optional<LoanAccount> findByIdForUpdate(@Param("id") Long id);
}
