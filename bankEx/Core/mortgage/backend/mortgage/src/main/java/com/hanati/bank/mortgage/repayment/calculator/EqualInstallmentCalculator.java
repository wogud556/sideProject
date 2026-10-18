package com.hanati.bank.mortgage.repayment.calculator;

import com.hanati.bank.mortgage.common.util.Money;
import com.hanati.bank.mortgage.limit.engine.AnnualDebtService;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/** 원리금균등: 매달 같은 금액을 내고, 그 안에서 이자를 뺀 나머지가 원금이다. */
public class EqualInstallmentCalculator implements RepaymentCalculator {

    @Override
    public List<ScheduleLine> calculate(long principal, BigDecimal annualRatePercent, int months) {
        BigDecimal r = MonthlyInterest.rate(annualRatePercent);
        long payment = Money.roundWon(BigDecimal.valueOf(principal)
                .multiply(AnnualDebtService.monthlyInstallmentFactor(r, months), MathContext.DECIMAL128));

        List<ScheduleLine> lines = new ArrayList<>(months);
        long balance = principal;
        for (int n = 1; n <= months; n++) {
            long interest = MonthlyInterest.of(balance, r);
            long principalPart = n == months ? balance : Math.min(balance, payment - interest);
            balance -= principalPart;
            lines.add(new ScheduleLine(n, principalPart, interest, balance));
        }
        return lines;
    }
}
