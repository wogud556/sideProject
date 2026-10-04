package com.hanati.bank.repayment.policy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 배분 계산 시점의 회차별 미납 잔액 스냅샷. 정책이 엔티티를 직접 만지지 않도록 분리한다.
 */
public record ScheduleSnapshot(
        Long scheduleId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal outstandingOverdueInterest,
        BigDecimal outstandingInterest,
        BigDecimal outstandingOverduePrincipal,
        BigDecimal outstandingPrincipal
) {
    public boolean isDue(LocalDate businessDate) {
        return !dueDate.isAfter(businessDate);
    }
}
