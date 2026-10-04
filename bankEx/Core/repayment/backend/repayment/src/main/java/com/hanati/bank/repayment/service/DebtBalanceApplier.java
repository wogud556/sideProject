package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.RepaymentSchedule;
import com.hanati.bank.repayment.enums.ScheduleStatus;
import com.hanati.bank.repayment.policy.AllocationLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 배분 결과를 계좌·스케줄 잔액에 반영한다. 정상 반영(sign = +1)과 취소 반영(sign = -1)이
 * 같은 코드를 쓰므로 역거래가 원거래와 정확히 대칭이 된다 (명세 3.7).
 *
 * <p>계좌의 연체원금·연체이자·미수이자는 스케줄에서 다시 합산해 갱신한다. 감산 방식이 아니라
 * 재집계 방식이라 반복 상환·취소에도 집계값이 어긋나지 않는다. 원금잔액과 비용잔액은
 * 스케줄에서 도출할 수 없으므로(중도상환 시 스케줄 미변경) 직접 증감시킨다.
 */
@Component
public class DebtBalanceApplier {

    public void apply(LoanAccount account, Map<Long, RepaymentSchedule> scheduleById,
                       List<RepaymentSchedule> allSchedules, List<AllocationLine> lines,
                       LocalDate businessDate, int sign) {

        for (AllocationLine line : lines) {
            BigDecimal amount = sign > 0 ? line.amount() : line.amount().negate();
            RepaymentSchedule schedule = line.scheduleId() == null ? null : scheduleById.get(line.scheduleId());

            switch (line.type()) {
                case FEE -> {
                    // 정상 반영: 기존 비용잔액 범위까지만 차감한다. 초과분은 이 거래에서 발생한
                    // 중도상환수수료이며 잔액으로 존재하지 않았으므로 차감할 대상도 없다.
                    // 취소 반영: restoreFeeBalance가 기존 비용잔액만 되살리므로 여기서는 손대지 않는다.
                    if (sign > 0) {
                        BigDecimal fromBalance = Money.minNonNegative(line.amount(), account.getFeeBalance());
                        account.setFeeBalance(account.getFeeBalance().subtract(fromBalance));
                    }
                }
                case OVERDUE_INTEREST -> {
                    requireSchedule(schedule);
                    schedule.setOverdueInterest(schedule.getOverdueInterest().subtract(amount));
                }
                case INTEREST -> {
                    requireSchedule(schedule);
                    schedule.setPaidInterest(schedule.getPaidInterest().add(amount));
                }
                case OVERDUE_PRINCIPAL -> {
                    requireSchedule(schedule);
                    schedule.setOverduePrincipal(schedule.getOverduePrincipal().subtract(amount));
                }
                case PRINCIPAL -> {
                    if (schedule != null) {
                        schedule.setPaidPrincipal(schedule.getPaidPrincipal().add(amount));
                    }
                    account.setPrincipalBalance(account.getPrincipalBalance().subtract(amount));
                }
                case OVERPAYMENT -> {
                    // 과오납은 채무 잔액을 바꾸지 않는다. 별도 Overpayment 원장으로만 관리한다.
                }
            }
        }

        refreshAggregates(account, allSchedules, businessDate);
        refreshScheduleStatuses(allSchedules);
        verifyNonNegative(account, allSchedules);
    }

    /** 취소 시 기존 비용잔액만 되살린다 (중도상환수수료 제외). */
    public void restoreFeeBalance(LoanAccount account, BigDecimal feeAllocated, BigDecimal prepaymentFee) {
        BigDecimal restorable = feeAllocated.subtract(Money.normalize(prepaymentFee));
        if (Money.isPositive(restorable)) {
            account.setFeeBalance(account.getFeeBalance().add(restorable));
        }
    }

    private void refreshAggregates(LoanAccount account, List<RepaymentSchedule> schedules, LocalDate businessDate) {
        BigDecimal overdueInterest = Money.ZERO;
        BigDecimal overduePrincipal = Money.ZERO;
        BigDecimal accruedInterest = Money.ZERO;

        for (RepaymentSchedule s : schedules) {
            overdueInterest = overdueInterest.add(s.getOverdueInterest());
            overduePrincipal = overduePrincipal.add(s.getOverduePrincipal());
            if (!s.getDueDate().isAfter(businessDate)) {
                accruedInterest = accruedInterest.add(s.outstandingInterest());
            }
        }
        account.setOverdueInterest(overdueInterest);
        account.setOverduePrincipal(overduePrincipal);
        account.setAccruedInterest(accruedInterest);
    }

    private void refreshScheduleStatuses(List<RepaymentSchedule> schedules) {
        LocalDateTime now = LocalDateTime.now();
        for (RepaymentSchedule s : schedules) {
            if (s.getStatus() == ScheduleStatus.CANCELLED) {
                continue;
            }
            if (Money.isZero(s.totalOutstanding())) {
                s.setStatus(ScheduleStatus.PAID);
                if (s.getPaidAt() == null) {
                    s.setPaidAt(now);
                }
            } else if (Money.isPositive(s.getOverduePrincipal()) || Money.isPositive(s.getOverdueInterest())) {
                s.setStatus(ScheduleStatus.OVERDUE);
                s.setPaidAt(null);
            } else if (Money.isPositive(s.getPaidPrincipal()) || Money.isPositive(s.getPaidInterest())) {
                s.setStatus(ScheduleStatus.PARTIALLY_PAID);
                s.setPaidAt(null);
            } else {
                s.setStatus(ScheduleStatus.SCHEDULED);
                s.setPaidAt(null);
            }
        }
    }

    /** 명세 9번: 원금·이자·비용 잔액은 음수가 될 수 없다. */
    private void verifyNonNegative(LoanAccount account, List<RepaymentSchedule> schedules) {
        if (account.getPrincipalBalance().signum() < 0
                || account.getFeeBalance().signum() < 0
                || account.getOverduePrincipal().signum() < 0
                || account.getOverdueInterest().signum() < 0
                || account.getAccruedInterest().signum() < 0) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALLOCATION_FAILED, "계좌 잔액이 음수가 되었습니다");
        }
        for (RepaymentSchedule s : schedules) {
            if (s.outstandingPrincipal().signum() < 0 || s.outstandingInterest().signum() < 0
                    || s.getOverduePrincipal().signum() < 0 || s.getOverdueInterest().signum() < 0) {
                throw new BusinessException(ErrorCode.REPAYMENT_ALLOCATION_FAILED,
                        "스케줄 " + s.getInstallmentNumber() + "회차 잔액이 음수가 되었습니다");
            }
        }
    }

    private void requireSchedule(RepaymentSchedule schedule) {
        if (schedule == null) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALLOCATION_FAILED, "회차 정보가 없는 배분 항목");
        }
    }
}
