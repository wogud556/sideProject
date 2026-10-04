package com.hanati.bank.repayment.config;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.RepaymentSchedule;
import com.hanati.bank.repayment.enums.LoanAccountStatus;
import com.hanati.bank.repayment.enums.ScheduleStatus;
import com.hanati.bank.repayment.gateway.DemoScenarioAccounts;
import com.hanati.bank.repayment.policy.DefaultRepaymentAllocationPolicy;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 데모 대출계좌 시드. 대출 실행은 이 코어의 책임이 아니므로 계좌 생성 API 대신 시드로 적재한다.
 * 연체 전이 로직이 범위 밖이므로 연체 잔액은 여기서 직접 설정한다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (loanAccountRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        // 1) 정상 계좌 — 12회차 중 1회차만 도래
        LoanAccount normal = save("LN-2026-0001", 1001L, today.minusMonths(1), 12,
                10_000_000L, 0L, 0L, 0L, BigDecimal.ZERO, "110-000-111111");
        seedEqualPrincipalSchedules(normal, today.minusMonths(1), 12, 10_000_000L, 45_000L);

        // 2) 연체 계좌 — 1·2회차 연체(연체원금/연체이자), 비용 10,000원
        LoanAccount delinquent = save("LN-2026-0002", 1002L, today.minusMonths(3), 12,
                10_000_000L, 0L, 0L, 10_000L, BigDecimal.ZERO, "110-000-222222");
        seedDelinquentSchedules(delinquent, today);

        // 3) 중도상환수수료 1.2% 계좌
        LoanAccount withFee = save("LN-2026-0003", 1003L, today.minusMonths(2), 12,
                10_000_000L, 0L, 0L, 0L, new BigDecimal("1.20"), "110-000-333333");
        seedEqualPrincipalSchedules(withFee, today.minusMonths(2), 12, 10_000_000L, 45_000L);

        // 4) 자동이체 출금이 항상 실패하는 계좌
        LoanAccount failing = save("LN-2026-0004", 1004L, today.minusMonths(1), 12,
                10_000_000L, 0L, 0L, 0L, BigDecimal.ZERO,
                DemoScenarioAccounts.WITHDRAWAL_FAILURE_ACCOUNT);
        seedEqualPrincipalSchedules(failing, today.minusMonths(1), 12, 10_000_000L, 45_000L);
    }

    private LoanAccount save(String accountNumber, Long customerId, LocalDate disbursementDate,
                              int termMonths, long principal, long overduePrincipal, long overdueInterest,
                              long feeBalance, BigDecimal prepaymentFeeRate, String withdrawalAccount) {
        return loanAccountRepository.save(LoanAccount.builder()
                .loanAccountNumber(accountNumber)
                .customerId(customerId)
                .productId(1L)
                .productType(DefaultRepaymentAllocationPolicy.PRODUCT_TYPE)
                .status(overduePrincipal > 0 ? LoanAccountStatus.DELINQUENT : LoanAccountStatus.ACTIVE)
                .originalPrincipal(Money.of(principal))
                .principalBalance(Money.of(principal))
                .overduePrincipal(Money.of(overduePrincipal))
                .accruedInterest(Money.ZERO)
                .overdueInterest(Money.of(overdueInterest))
                .feeBalance(Money.of(feeBalance))
                .prepaymentFeeRate(prepaymentFeeRate.setScale(2))
                .disbursementDate(disbursementDate)
                .maturityDate(disbursementDate.plusMonths(termMonths))
                .withdrawalAccountNumber(withdrawalAccount)
                .build());
    }

    /** 원금균등: 회차 원금 = 원금/회차수, 이자는 데모용 고정액. */
    private void seedEqualPrincipalSchedules(LoanAccount account, LocalDate firstDueDate,
                                              int installments, long principal, long interestPerInstallment) {
        long principalPerInstallment = principal / installments;
        List<RepaymentSchedule> rows = new ArrayList<>();
        for (int i = 1; i <= installments; i++) {
            long p = (i == installments)
                    ? principal - principalPerInstallment * (installments - 1)
                    : principalPerInstallment;
            rows.add(schedule(account, i, firstDueDate.plusMonths(i - 1L), p, interestPerInstallment,
                    0L, 0L, ScheduleStatus.SCHEDULED));
        }
        scheduleRepository.saveAll(rows);
    }

    /**
     * 1·2회차는 연체(정상 버킷을 비우고 연체 버킷으로 옮긴 상태), 3회차부터는 정상.
     * 연체 전이 로직이 없으므로 두 버킷이 겹치지 않도록 여기서 직접 분리해 둔다.
     */
    private void seedDelinquentSchedules(LoanAccount account, LocalDate today) {
        List<RepaymentSchedule> rows = new ArrayList<>();
        rows.add(schedule(account, 1, today.minusMonths(2), 0L, 0L, 833_333L, 20_000L, ScheduleStatus.OVERDUE));
        rows.add(schedule(account, 2, today.minusMonths(1), 0L, 0L, 833_333L, 15_000L, ScheduleStatus.OVERDUE));
        for (int i = 3; i <= 12; i++) {
            long p = (i == 12) ? 10_000_000L - 833_333L * 11 : 833_333L;
            rows.add(schedule(account, i, today.plusMonths(i - 3L), p, 45_000L, 0L, 0L, ScheduleStatus.SCHEDULED));
        }
        scheduleRepository.saveAll(rows);

        long overduePrincipal = 833_333L * 2;
        account.setOverduePrincipal(Money.of(overduePrincipal));
        account.setOverdueInterest(Money.of(35_000L));
        // 연체로 이전된 원금은 정상 원금잔액에서 빠진다 (두 버킷은 서로 배타적이다).
        account.setPrincipalBalance(Money.of(10_000_000L - overduePrincipal));
        account.setStatus(LoanAccountStatus.DELINQUENT);
        loanAccountRepository.save(account);
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
