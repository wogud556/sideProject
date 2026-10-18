package com.hanati.bank.mortgage.repayment.calculator;

import com.hanati.bank.mortgage.common.util.Money;
import com.hanati.bank.mortgage.limit.engine.AnnualDebtService;

import java.math.BigDecimal;
import java.math.MathContext;

final class MonthlyInterest {

    private MonthlyInterest() {
    }

    static long of(long balance, BigDecimal monthlyRate) {
        return Money.roundWon(BigDecimal.valueOf(balance).multiply(monthlyRate, MathContext.DECIMAL128));
    }

    static BigDecimal rate(BigDecimal annualRatePercent) {
        return AnnualDebtService.monthlyRate(annualRatePercent);
    }
}
