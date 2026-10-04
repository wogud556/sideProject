package com.hanati.bank.repayment.enums;

/** 명세 4번의 배분 우선순위. order 값이 작을수록 먼저 배분한다. */
public enum AllocationType {
    FEE(1),
    OVERDUE_INTEREST(2),
    INTEREST(3),
    OVERDUE_PRINCIPAL(4),
    PRINCIPAL(5),
    OVERPAYMENT(6);

    private final int order;

    AllocationType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
