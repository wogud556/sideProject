package com.hanati.bank.mortgage.repayment.calculator;

/** 상환 예정표 한 회차. balanceAfter는 이 회차를 낸 뒤 남는 원금이다. */
public record ScheduleLine(int installmentNo, long principal, long interest, long balanceAfter) {

    public long payment() {
        return principal + interest;
    }
}
