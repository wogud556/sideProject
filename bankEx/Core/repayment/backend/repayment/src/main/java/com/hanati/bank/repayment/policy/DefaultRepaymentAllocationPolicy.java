package com.hanati.bank.repayment.policy;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.enums.AllocationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 기본 배분 정책 (명세 4번).
 *
 * <p>배분 순서는 다음과 같다.
 * <ol>
 *   <li>계좌 비용 + 중도상환수수료 (FEE)</li>
 *   <li>납부예정일이 도래한 회차를 오래된 순으로: 연체이자 → 정상이자 → 연체원금 → 정상원금</li>
 *   <li>거래유형이 미래 원금을 허용하면(PREPAYMENT / FULL_REPAYMENT) 남은 원금잔액에 배분.
 *       그렇지 않으면 미래 회차에 선납(정상이자 → 정상원금)한다.</li>
 *   <li>그래도 남으면 과오납 (OVERPAYMENT)</li>
 * </ol>
 *
 * <p>2단계의 "도래 회차"는 {@code dueDate <= businessDate}로 판정한다. 중도상환·전액상환이
 * 아직 도래하지 않은 이자를 선취하지 않도록 하기 위함이며, 명세 3.6의 전액상환 구성 항목에도
 * 미래 이자가 포함되지 않는다.
 *
 * <p>중도상환은 "스케줄 미변경, 원금만 감소" 정책이므로 3단계 배분은 회차에 기록하지 않고
 * 계좌 원금잔액만 줄인다(scheduleId = null).
 */
@Component
public class DefaultRepaymentAllocationPolicy implements RepaymentAllocationPolicy {

    public static final String PRODUCT_TYPE = "DEFAULT";

    @Override
    public String supportedProductType() {
        return PRODUCT_TYPE;
    }

    @Override
    public RepaymentAllocationResult allocate(RepaymentAllocationContext context) {
        List<AllocationLine> lines = new ArrayList<>();
        BigDecimal remaining = Money.normalize(context.getPaymentAmount());

        // 원금잔액은 회차 배분과 미래원금 배분이 함께 소비하므로 하나의 한도로 추적한다.
        // 중도상환으로 원금잔액이 줄어도 회차 예정원금은 그대로 남기 때문에(스케줄 미변경 정책),
        // 이 한도가 없으면 원금잔액이 음수가 될 수 있다 (명세 9번).
        BigDecimal principalLimit = Money.normalize(context.getPrincipalBalance());

        // 1) 비용 + 중도상환수수료
        BigDecimal feeDue = Money.normalize(context.getFeeBalance())
                .add(Money.normalize(context.getPrepaymentFee()));
        remaining = take(lines, null, AllocationType.FEE, feeDue, remaining);

        // 2) 도래한 회차: 오래된 순
        for (ScheduleSnapshot s : context.getSchedules()) {
            if (!s.isDue(context.getBusinessDate())) {
                continue;
            }
            if (!Money.isPositive(remaining)) {
                break;
            }
            remaining = take(lines, s.scheduleId(), AllocationType.OVERDUE_INTEREST,
                    s.outstandingOverdueInterest(), remaining);
            remaining = take(lines, s.scheduleId(), AllocationType.INTEREST,
                    s.outstandingInterest(), remaining);

            BigDecimal overduePrincipal = Money.minNonNegative(s.outstandingOverduePrincipal(), remaining);
            remaining = take(lines, s.scheduleId(), AllocationType.OVERDUE_PRINCIPAL, overduePrincipal, remaining);

            BigDecimal principal = Money.minNonNegative(s.outstandingPrincipal(), principalLimit);
            BigDecimal before = remaining;
            remaining = take(lines, s.scheduleId(), AllocationType.PRINCIPAL, principal, remaining);
            principalLimit = principalLimit.subtract(before.subtract(remaining));
        }

        // 3) 미래 구간
        if (Money.isPositive(remaining)) {
            if (context.getTransactionType().allowsFuturePrincipal()) {
                BigDecimal before = remaining;
                remaining = take(lines, null, AllocationType.PRINCIPAL, principalLimit, remaining);
                principalLimit = principalLimit.subtract(before.subtract(remaining));
            } else {
                for (ScheduleSnapshot s : context.getSchedules()) {
                    if (s.isDue(context.getBusinessDate())) {
                        continue;
                    }
                    if (!Money.isPositive(remaining)) {
                        break;
                    }
                    remaining = take(lines, s.scheduleId(), AllocationType.INTEREST,
                            s.outstandingInterest(), remaining);

                    BigDecimal principal = Money.minNonNegative(s.outstandingPrincipal(), principalLimit);
                    BigDecimal before = remaining;
                    remaining = take(lines, s.scheduleId(), AllocationType.PRINCIPAL, principal, remaining);
                    principalLimit = principalLimit.subtract(before.subtract(remaining));
                }
            }
        }

        // 4) 과오납
        if (Money.isPositive(remaining)) {
            lines.add(new AllocationLine(null, AllocationType.OVERPAYMENT, remaining, lines.size() + 1));
        }

        return new RepaymentAllocationResult(lines);
    }

    /**
     * due 중 remaining 범위에서 배분하고 남은 금액을 돌려준다. 0원 배분은 행을 만들지 않는다.
     *
     * <p>order는 타입 우선순위가 아니라 이 거래 안에서의 처리 순번이다. 타입 우선순위는
     * {@link AllocationType#getOrder()}가 이미 표현하므로, 원장에는 "몇 번째로 배분했는지"를
     * 남겨 회차별 처리 순서(명세 5번)를 재현할 수 있게 한다.
     */
    private BigDecimal take(List<AllocationLine> lines, Long scheduleId, AllocationType type,
                             BigDecimal due, BigDecimal remaining) {
        BigDecimal amount = Money.minNonNegative(Money.normalize(due), remaining);
        if (!Money.isPositive(amount)) {
            return remaining;
        }
        lines.add(new AllocationLine(scheduleId, type, amount, lines.size() + 1));
        return remaining.subtract(amount);
    }
}
