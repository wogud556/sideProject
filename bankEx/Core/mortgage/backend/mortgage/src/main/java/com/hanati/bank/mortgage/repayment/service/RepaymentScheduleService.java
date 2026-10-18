package com.hanati.bank.mortgage.repayment.service;

import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import com.hanati.bank.mortgage.repayment.calculator.RepaymentCalculator;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;
import com.hanati.bank.mortgage.repayment.enums.ScheduleStatus;
import com.hanati.bank.mortgage.repayment.repository.MortgageRepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    private final MortgageRepaymentScheduleRepository scheduleRepository;

    /** 실행일 기준 매월 같은 날을 납기일로 하는 예정표를 만든다 (휴일 조정은 하지 않는다). */
    public List<MortgageRepaymentSchedule> createSchedule(MortgageLoanAccount account) {
        return RepaymentCalculator.of(account.getRepaymentMethod())
                .calculate(account.getPrincipal(), account.getInterestRate(), account.getTermMonths()).stream()
                .map(line -> scheduleRepository.save(MortgageRepaymentSchedule.builder()
                        .loanAccountId(account.getLoanAccountId())
                        .installmentNo(line.installmentNo())
                        .dueDate(account.getExecutionDate().plusMonths(line.installmentNo()))
                        .principal(line.principal())
                        .interest(line.interest())
                        .totalPayment(line.payment())
                        .balanceAfter(line.balanceAfter())
                        .status(ScheduleStatus.SCHEDULED)
                        .build()))
                .toList();
    }
}
