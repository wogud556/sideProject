package com.hanati.bank.repayment.policy;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.enums.AllocationType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 배분 결과. 엔티티를 바꾸지 않고 "무엇을 얼마 배분할지"만 담는다. */
@Getter
public class RepaymentAllocationResult {

    private final List<AllocationLine> lines;
    private final BigDecimal allocatedAmount;
    private final BigDecimal overpaymentAmount;

    public RepaymentAllocationResult(List<AllocationLine> lines) {
        this.lines = List.copyOf(lines);
        this.allocatedAmount = lines.stream()
                .filter(l -> l.type() != AllocationType.OVERPAYMENT)
                .map(AllocationLine::amount)
                .reduce(Money.ZERO, BigDecimal::add);
        this.overpaymentAmount = lines.stream()
                .filter(l -> l.type() == AllocationType.OVERPAYMENT)
                .map(AllocationLine::amount)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    public BigDecimal totalOf(AllocationType type) {
        return lines.stream()
                .filter(l -> l.type() == type)
                .map(AllocationLine::amount)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    /** 배분금액 + 과오납 = 요청금액 (명세 13.4 불변조건). */
    public BigDecimal total() {
        return allocatedAmount.add(overpaymentAmount);
    }
}
