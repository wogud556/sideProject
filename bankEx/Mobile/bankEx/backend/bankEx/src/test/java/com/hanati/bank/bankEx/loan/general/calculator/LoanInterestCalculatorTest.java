package com.hanati.bank.bankEx.loan.general.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoanInterestCalculatorTest {

    // ---------- calculateMonthlyInterest ----------

    @Test
    void monthlyInterest_annualRate4_8_principal10M_returns40000() {
        // 연 4.8%, 잔여원금 10,000,000 → 월이자 = 10,000,000 * (4.8 / 1200) = 40,000
        BigDecimal result = LoanInterestCalculator.calculateMonthlyInterest(
                BigDecimal.valueOf(10_000_000L),
                BigDecimal.valueOf(4.8));
        assertEquals(new BigDecimal("40000"), result);
    }

    @Test
    void monthlyInterest_annualRate3_6_principal5M_returns15000() {
        // 연 3.6%, 잔여원금 5,000,000 → 월이자 = 5,000,000 * (3.6 / 1200) = 15,000
        BigDecimal result = LoanInterestCalculator.calculateMonthlyInterest(
                BigDecimal.valueOf(5_000_000L),
                BigDecimal.valueOf(3.6));
        assertEquals(new BigDecimal("15000"), result);
    }

    // ---------- calculateEqualPrincipalAmount ----------

    @Test
    void equalPrincipal_10M_12months_returns833333_halfUp() {
        // 10,000,000 / 12 = 833,333.333... → HALF_UP → 833,333
        BigDecimal result = LoanInterestCalculator.calculateEqualPrincipalAmount(
                BigDecimal.valueOf(10_000_000L), 12);
        assertEquals(new BigDecimal("833333"), result);
    }

    @Test
    void equalPrincipal_10M_3months_returns3333333_halfUp() {
        // 10,000,000 / 3 = 3,333,333.333... → HALF_UP → 3,333,333
        // 마지막 회차 나머지 처리는 서비스 담당
        BigDecimal result = LoanInterestCalculator.calculateEqualPrincipalAmount(
                BigDecimal.valueOf(10_000_000L), 3);
        assertEquals(new BigDecimal("3333333"), result);
    }
}
