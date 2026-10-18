package com.hanati.bank.mortgage.repayment.repository;

import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;
import com.hanati.bank.mortgage.repayment.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MortgageRepaymentScheduleRepository extends JpaRepository<MortgageRepaymentSchedule, Long> {

    List<MortgageRepaymentSchedule> findByLoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);

    Optional<MortgageRepaymentSchedule> findFirstByLoanAccountIdAndStatusOrderByInstallmentNoAsc(Long loanAccountId,
                                                                                                ScheduleStatus status);
}
