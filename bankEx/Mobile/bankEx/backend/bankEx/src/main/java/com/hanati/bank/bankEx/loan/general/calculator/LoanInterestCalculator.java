package com.hanati.bank.bankEx.loan.general.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanInterestCalculator {

    private LoanInterestCalculator() {}

    // 월 이자 계산
    // annualRatePercent: 연 이자율 (예: 3.5)
    // remainingPrincipal: 잔여 원금
    public static BigDecimal calculateMonthlyInterest(BigDecimal remainingPrincipal, BigDecimal annualRatePercent) {
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);
        return remainingPrincipal.multiply(monthlyRate).setScale(0, RoundingMode.HALF_UP);
    }

    // EQUAL_PRINCIPAL: 회차별 고정 원금
    public static BigDecimal calculateEqualPrincipalAmount(BigDecimal totalPrincipal, int loanPeriod) {
        return totalPrincipal.divide(BigDecimal.valueOf(loanPeriod), 0, RoundingMode.HALF_UP);
    }
}
