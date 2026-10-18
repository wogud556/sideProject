package com.hanati.bank.mortgage.repayment.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 원금균등: 매달 같은 원금을 갚고, 이자는 줄어드는 잔액에 붙는다. */
public class EqualPrincipalCalculator implements RepaymentCalculator {

    @Override
    public List<ScheduleLine> calculate(long principal, BigDecimal annualRatePercent, int months) {
        BigDecimal r = MonthlyInterest.rate(annualRatePercent);
        long principalPart = principal / months;

        List<ScheduleLine> lines = new ArrayList<>(months);
        long balance = principal;
        for (int n = 1; n <= months; n++) {
            long interest = MonthlyInterest.of(balance, r);
            long paid = n == months ? balance : principalPart;
            balance -= paid;
            lines.add(new ScheduleLine(n, paid, interest, balance));
        }
        return lines;
    }
}
