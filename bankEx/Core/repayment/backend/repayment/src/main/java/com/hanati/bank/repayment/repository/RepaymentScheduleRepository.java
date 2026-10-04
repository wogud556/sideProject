package com.hanati.bank.repayment.repository;

import com.hanati.bank.repayment.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    /**
     * 명세 5번의 정렬 기준: 납부예정일 오름차순 → 회차 오름차순 → 스케줄 ID 오름차순.
     * 동일 납부예정일 스케줄이 여러 건이어도 처리 순서가 결정적이다.
     */
    List<RepaymentSchedule> findByLoanAccountIdOrderByDueDateAscInstallmentNumberAscIdAsc(Long loanAccountId);
}
