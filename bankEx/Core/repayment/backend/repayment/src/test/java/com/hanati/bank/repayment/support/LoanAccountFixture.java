package com.hanati.bank.repayment.support;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.RepaymentSchedule;
import com.hanati.bank.repayment.enums.LoanAccountStatus;
import com.hanati.bank.repayment.enums.ScheduleStatus;
import com.hanati.bank.repayment.policy.DefaultRepaymentAllocationPolicy;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** 테스트용 대출계좌·스케줄 생성기. 연체 전이 로직이 없으므로 연체 잔액을 직접 주입한다. */
@Component
@RequiredArgsConstructor
public class LoanAccountFixture {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    public LoanAccount account(long principal, long feeBalance, BigDecimal prepaymentFeeRate,
                                String withdrawalAccount, LocalDate disbursementDate) {
        return loanAccountRepository.saveAndFlush(LoanAccount.builder()
                .loanAccountNumber("TST-" + SEQ.incrementAndGet() + "-" + System.nanoTime())
                .customerId(9001L)
                .productId(1L)
                .productType(DefaultRepaymentAllocationPolicy.PRODUCT_TYPE)
                .status(LoanAccountStatus.ACTIVE)
                .originalPrincipal(Money.of(principal))
                .principalBalance(Money.of(principal))
                .overduePrincipal(Money.ZERO)
                .accruedInterest(Money.ZERO)
                .overdueInterest(Money.ZERO)
                .feeBalance(Money.of(feeBalance))
                .prepaymentFeeRate(prepaymentFeeRate.setScale(2))
                .disbursementDate(disbursementDate)
                .maturityDate(disbursementDate.plusMonths(12))
                .withdrawalAccountNumber(withdrawalAccount)
                .build());
    }

    public LoanAccount simpleAccount(long principal) {
        return account(principal, 0L, BigDecimal.ZERO, "110-000-111111", LocalDate.now().minusMonths(1));
    }

    /** 도래한 회차 1건만 가진 계좌. */
    public RepaymentSchedule dueSchedule(LoanAccount account, long principal, long interest) {
        return scheduleRepository.saveAndFlush(schedule(account, 1, LocalDate.now(),
                principal, interest, 0L, 0L, ScheduleStatus.SCHEDULED));
    }

    /** 원금균등 스케줄 n건. 첫 회차는 firstDueDate. */
    public List<RepaymentSchedule> equalPrincipalSchedules(LoanAccount account, int installments,
                                                            long principal, long interestPerInstallment,
                                                            LocalDate firstDueDate) {
        long per = principal / installments;
        List<RepaymentSchedule> rows = new ArrayList<>();
        for (int i = 1; i <= installments; i++) {
            long p = (i == installments) ? principal - per * (installments - 1) : per;
            rows.add(schedule(account, i, firstDueDate.plusMonths(i - 1L), p, interestPerInstallment,
                    0L, 0L, ScheduleStatus.SCHEDULED));
        }
        return scheduleRepository.saveAllAndFlush(rows);
    }

    public RepaymentSchedule overdueSchedule(LoanAccount account, int seq, LocalDate dueDate,
                                              long overduePrincipal, long overdueInterest) {
        RepaymentSchedule saved = scheduleRepository.saveAndFlush(schedule(account, seq, dueDate,
                0L, 0L, overduePrincipal, overdueInterest, ScheduleStatus.OVERDUE));
        // 연체로 이전된 원금은 정상 원금잔액에서 뺀다 (두 버킷은 배타적이다).
        account.setPrincipalBalance(account.getPrincipalBalance().subtract(Money.of(overduePrincipal)));
        account.setOverduePrincipal(account.getOverduePrincipal().add(Money.of(overduePrincipal)));
        account.setOverdueInterest(account.getOverdueInterest().add(Money.of(overdueInterest)));
        account.setStatus(LoanAccountStatus.DELINQUENT);
        loanAccountRepository.saveAndFlush(account);
        return saved;
    }

    private RepaymentSchedule schedule(LoanAccount account, int seq, LocalDate dueDate,
                                        long scheduledPrincipal, long scheduledInterest,
                                        long overduePrincipal, long overdueInterest, ScheduleStatus status) {
        return RepaymentSchedule.builder()
                .loanAccountId(account.getId())
                .installmentNumber(seq)
                .dueDate(dueDate)
                .scheduledPrincipal(Money.of(scheduledPrincipal))
                .scheduledInterest(Money.of(scheduledInterest))
                .paidPrincipal(Money.ZERO)
                .paidInterest(Money.ZERO)
                .overduePrincipal(Money.of(overduePrincipal))
                .overdueInterest(Money.of(overdueInterest))
                .status(status)
                .build();
    }
}
