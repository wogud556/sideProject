package com.hanati.bank.bankEx.loan.jeonse.service;

import org.springframework.stereotype.Service;

@Service
public class jeonseLimitCalculator {

    private static final double DEPOSIT_LIMIT_RATIO = 0.8;
    private static final double INCOME_LIMIT_MULTIPLIER = 3.5;

    public long calculate(long depositAmount, long annualIncome, long existingDebtAmount, long productMaxLimitAmount) {
        long depositLimit = Math.round(depositAmount * DEPOSIT_LIMIT_RATIO);
        long incomeLimit = Math.round(annualIncome * INCOME_LIMIT_MULTIPLIER);
        long debtDeductedLimit = incomeLimit - existingDebtAmount;

        long finalLimit = Math.min(Math.min(depositLimit, debtDeductedLimit), productMaxLimitAmount);
        return Math.max(finalLimit, 0L);
    }
}
