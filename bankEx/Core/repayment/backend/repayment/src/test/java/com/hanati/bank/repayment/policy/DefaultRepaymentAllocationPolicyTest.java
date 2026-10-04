package com.hanati.bank.repayment.policy;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.enums.AllocationType;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRepaymentAllocationPolicyTest {

    private final DefaultRepaymentAllocationPolicy policy = new DefaultRepaymentAllocationPolicy();

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    /** 납부예정일이 도래한 회차 하나. */
    private ScheduleSnapshot due(long id, long overdueInterest, long interest,
                                  long overduePrincipal, long principal) {
        return new ScheduleSnapshot(id, 1, TODAY.minusMonths(1),
                Money.of(overdueInterest), Money.of(interest),
                Money.of(overduePrincipal), Money.of(principal));
    }

    private ScheduleSnapshot dueOn(long id, int seq, LocalDate dueDate, long interest, long principal) {
        return new ScheduleSnapshot(id, seq, dueDate, Money.ZERO, Money.of(interest), Money.ZERO, Money.of(principal));
    }

    private RepaymentAllocationContext.RepaymentAllocationContextBuilder ctx(long payment) {
        return RepaymentAllocationContext.builder()
                .transactionType(RepaymentTransactionType.REGULAR_REPAYMENT)
                .businessDate(TODAY)
                .paymentAmount(Money.of(payment))
                .feeBalance(Money.ZERO)
                .prepaymentFee(Money.ZERO)
                .principalBalance(Money.ZERO)
                .schedules(List.of());
    }

    // ---------- 명세 4번의 예시 ----------

    @Test
    void 명세예시_10만원납부시_연체원금4만까지만배분되고_1만원이남는다() {
        // 비용 1만 / 연체이자 2만 / 정상이자 3만 / 연체원금 5만, 납부 10만
        RepaymentAllocationResult r = policy.allocate(ctx(100_000)
                .feeBalance(Money.of(10_000))
                .principalBalance(Money.ZERO)
                .schedules(List.of(due(1L, 20_000, 30_000, 50_000, 0)))
                .build());

        assertEquals(Money.of(10_000), r.totalOf(AllocationType.FEE));
        assertEquals(Money.of(20_000), r.totalOf(AllocationType.OVERDUE_INTEREST));
        assertEquals(Money.of(30_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.of(40_000), r.totalOf(AllocationType.OVERDUE_PRINCIPAL));
        assertEquals(Money.of(100_000), r.getAllocatedAmount());
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }

    // ---------- 단계별 부분 배분 ----------

    @Test
    void 비용만_전액상환() {
        RepaymentAllocationResult r = policy.allocate(ctx(10_000)
                .feeBalance(Money.of(10_000))
                .schedules(List.of(due(1L, 20_000, 30_000, 50_000, 0)))
                .build());

        assertEquals(Money.of(10_000), r.totalOf(AllocationType.FEE));
        assertEquals(Money.ZERO, r.totalOf(AllocationType.OVERDUE_INTEREST));
        assertEquals(1, r.getLines().size());
    }

    @Test
    void 비용과_연체이자까지_상환() {
        RepaymentAllocationResult r = policy.allocate(ctx(30_000)
                .feeBalance(Money.of(10_000))
                .schedules(List.of(due(1L, 20_000, 30_000, 50_000, 0)))
                .build());

        assertEquals(Money.of(10_000), r.totalOf(AllocationType.FEE));
        assertEquals(Money.of(20_000), r.totalOf(AllocationType.OVERDUE_INTEREST));
        assertEquals(Money.ZERO, r.totalOf(AllocationType.INTEREST));
    }

    @Test
    void 정상이자까지_상환() {
        RepaymentAllocationResult r = policy.allocate(ctx(60_000)
                .feeBalance(Money.of(10_000))
                .schedules(List.of(due(1L, 20_000, 30_000, 50_000, 0)))
                .build());

        assertEquals(Money.of(30_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.ZERO, r.totalOf(AllocationType.OVERDUE_PRINCIPAL));
    }

    @Test
    void 일부원금만_상환() {
        RepaymentAllocationResult r = policy.allocate(ctx(70_000)
                .principalBalance(Money.of(1_000_000))
                .schedules(List.of(due(1L, 0, 50_000, 0, 500_000)))
                .build());

        assertEquals(Money.of(50_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.of(20_000), r.totalOf(AllocationType.PRINCIPAL));
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }

    @Test
    void 모든채무_상환시_과오납없음() {
        RepaymentAllocationResult r = policy.allocate(ctx(560_000)
                .feeBalance(Money.of(10_000))
                .principalBalance(Money.of(500_000))
                .schedules(List.of(due(1L, 0, 50_000, 0, 500_000)))
                .build());

        assertEquals(Money.of(560_000), r.getAllocatedAmount());
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }

    @Test
    void 채무보다_큰금액납부시_초과분은_과오납() {
        RepaymentAllocationResult r = policy.allocate(ctx(600_000)
                .feeBalance(Money.of(10_000))
                .principalBalance(Money.of(500_000))
                .schedules(List.of(due(1L, 0, 50_000, 0, 500_000)))
                .build());

        assertEquals(Money.of(560_000), r.getAllocatedAmount());
        assertEquals(Money.of(40_000), r.getOverpaymentAmount());
        assertEquals(Money.of(600_000), r.total());
    }

    @Test
    void 납부금액_0원이면_배분행이_없다() {
        RepaymentAllocationResult r = policy.allocate(ctx(0)
                .feeBalance(Money.of(10_000))
                .schedules(List.of(due(1L, 20_000, 30_000, 0, 0)))
                .build());

        assertTrue(r.getLines().isEmpty());
        assertEquals(Money.ZERO, r.getAllocatedAmount());
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }

    @Test
    void 음수_납부금액이면_배분행이_없다() {
        // 서비스 계층에서 먼저 INVALID_REPAYMENT_AMOUNT로 거르지만, 정책 자체도 음수를 흘리지 않는다.
        RepaymentAllocationResult r = policy.allocate(ctx(0)
                .paymentAmount(Money.of(-50_000))
                .feeBalance(Money.of(10_000))
                .build());

        assertTrue(r.getLines().isEmpty());
    }

    @Test
    void 배분합계와_과오납합계는_항상_요청금액과_같다() {
        for (long payment : new long[]{1, 9_999, 10_000, 55_555, 560_000, 1_000_000}) {
            RepaymentAllocationResult r = policy.allocate(ctx(payment)
                    .feeBalance(Money.of(10_000))
                    .principalBalance(Money.of(500_000))
                    .schedules(List.of(due(1L, 0, 50_000, 0, 500_000)))
                    .build());
            assertEquals(Money.of(payment), r.total(), "payment=" + payment);
        }
    }

    // ---------- 명세 5번: 회차 순서 ----------

    @Test
    void 미납회차가_여러개면_오래된_회차부터_처리한다() {
        ScheduleSnapshot first = dueOn(10L, 1, TODAY.minusMonths(2), 30_000, 100_000);
        ScheduleSnapshot second = dueOn(20L, 2, TODAY.minusMonths(1), 30_000, 100_000);

        // 1회차 전액(13만) + 2회차 이자 3만 = 16만
        RepaymentAllocationResult r = policy.allocate(ctx(160_000)
                .principalBalance(Money.of(200_000))
                .schedules(List.of(first, second))
                .build());

        List<AllocationLine> lines = r.getLines();
        assertEquals(10L, lines.get(0).scheduleId());
        assertEquals(AllocationType.INTEREST, lines.get(0).type());
        assertEquals(10L, lines.get(1).scheduleId());
        assertEquals(AllocationType.PRINCIPAL, lines.get(1).type());
        assertEquals(20L, lines.get(2).scheduleId());
        assertEquals(AllocationType.INTEREST, lines.get(2).type());
        assertEquals(Money.of(30_000), lines.get(2).amount());
        assertEquals(3, lines.size());
    }

    @Test
    void 도래하지않은_회차는_정기상환에서_도래회차보다_뒤에_배분된다() {
        ScheduleSnapshot dueNow = dueOn(10L, 1, TODAY, 10_000, 100_000);
        ScheduleSnapshot future = dueOn(20L, 2, TODAY.plusMonths(1), 10_000, 100_000);

        RepaymentAllocationResult r = policy.allocate(ctx(130_000)
                .principalBalance(Money.of(200_000))
                .schedules(List.of(dueNow, future))
                .build());

        // 도래분 11만을 먼저 채우고 남은 2만이 미래 회차 이자 1만 + 원금 1만으로 선납된다
        assertEquals(Money.of(20_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.of(110_000), r.totalOf(AllocationType.PRINCIPAL));
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }

    // ---------- 중도상환 / 전액상환 ----------

    @Test
    void 중도상환은_미래이자를_선취하지않고_원금만_감소시킨다() {
        ScheduleSnapshot dueNow = dueOn(10L, 1, TODAY, 10_000, 100_000);
        ScheduleSnapshot future = dueOn(20L, 2, TODAY.plusMonths(1), 10_000, 100_000);

        RepaymentAllocationResult r = policy.allocate(ctx(1_000_000)
                .transactionType(RepaymentTransactionType.PREPAYMENT)
                .principalBalance(Money.of(900_000))
                .schedules(List.of(dueNow, future))
                .build());

        // 도래분 이자 1만만 수취, 미래 회차 이자는 건드리지 않음
        assertEquals(Money.of(10_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.of(900_000), r.totalOf(AllocationType.PRINCIPAL));
        assertEquals(Money.of(90_000), r.getOverpaymentAmount());
    }

    @Test
    void 중도상환_원금배분은_원금잔액을_초과하지않는다() {
        // 스케줄 예정원금 합계(100만)가 원금잔액(30만)보다 큰 상황 (이전 중도상환으로 원금만 감소)
        ScheduleSnapshot s = dueOn(10L, 1, TODAY, 0, 1_000_000);

        RepaymentAllocationResult r = policy.allocate(ctx(1_000_000)
                .transactionType(RepaymentTransactionType.PREPAYMENT)
                .principalBalance(Money.of(300_000))
                .schedules(List.of(s))
                .build());

        assertEquals(Money.of(300_000), r.totalOf(AllocationType.PRINCIPAL));
        assertEquals(Money.of(700_000), r.getOverpaymentAmount());
    }

    @Test
    void 전액상환은_중도상환수수료를_비용과함께_배분한다() {
        ScheduleSnapshot s = dueOn(10L, 1, TODAY, 45_000, 0);

        RepaymentAllocationResult r = policy.allocate(ctx(10_095_000)
                .transactionType(RepaymentTransactionType.FULL_REPAYMENT)
                .feeBalance(Money.ZERO)
                .prepaymentFee(Money.of(50_000))
                .principalBalance(Money.of(10_000_000))
                .schedules(List.of(s))
                .build());

        assertEquals(Money.of(50_000), r.totalOf(AllocationType.FEE));
        assertEquals(Money.of(45_000), r.totalOf(AllocationType.INTEREST));
        assertEquals(Money.of(10_000_000), r.totalOf(AllocationType.PRINCIPAL));
        assertEquals(Money.ZERO, r.getOverpaymentAmount());
    }
}
