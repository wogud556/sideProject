package com.hanati.bank.repayment.enums;

public enum RepaymentTransactionType {
    REGULAR_REPAYMENT,
    MANUAL_REPAYMENT,
    PARTIAL_REPAYMENT,
    PREPAYMENT,
    FULL_REPAYMENT,
    REPAYMENT_REVERSAL,
    OVERPAYMENT_REFUND;

    /** 약정 스케줄을 넘어 미래 원금까지 배분할 수 있는 거래인지. */
    public boolean allowsFuturePrincipal() {
        return this == PREPAYMENT || this == FULL_REPAYMENT;
    }
}
