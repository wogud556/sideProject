package com.hanati.bank.mortgage.repayment.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 만기일시: 매달 이자만 내고 원금은 마지막 회차에 한 번에 갚는다. */
public class BulletRepaymentCalculator implements RepaymentCalculator {

    @Override
    public List<ScheduleLine> calculate(long principal, BigDecimal annualRatePercent, int months) {
        long interest = MonthlyInterest.of(principal, MonthlyInterest.rate(annualRatePercent));

        List<ScheduleLine> lines = new ArrayList<>(months);
        for (int n = 1; n < months; n++) {
            lines.add(new ScheduleLine(n, 0L, interest, principal));
        }
        lines.add(new ScheduleLine(months, principal, interest, 0L));
        return lines;
    }
}
