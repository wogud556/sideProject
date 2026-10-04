package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.RepaymentTransaction;
import com.hanati.bank.repayment.enums.RepaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentTransactionRepository extends JpaRepository<RepaymentTransaction, Long> {

    Optional<RepaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<RepaymentTransaction> findByTransactionNumber(String transactionNumber);

    boolean existsByTransactionNumber(String transactionNumber);

    List<RepaymentTransaction> findByLoanAccountIdOrderByIdDesc(Long loanAccountId);

    /** 원거래 이후에 해당 계좌에서 완료된 다른 거래가 있는지 (명세 7.3의 후속 거래 확인). */
    boolean existsByLoanAccountIdAndStatusAndIdGreaterThan(Long loanAccountId,
                                                            RepaymentTransactionStatus status,
                                                            Long id);
}
