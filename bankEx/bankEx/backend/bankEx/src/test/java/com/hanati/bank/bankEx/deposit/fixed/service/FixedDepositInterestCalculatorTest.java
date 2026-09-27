package com.hanati.bank.bankEx.deposit.fixed.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedDepositInterestCalculatorTest {

    private final FixedDepositInterestCalculator calculator = new FixedDepositInterestCalculator();

    // ---------- calculateInterest ----------

    @Test
    void maturityInterest_10M_rate3_5_12months() {
        // 10,000,000 * 3.5% * 12/12 = 350,000
        assertEquals(350_000L, calculator.calculateInterest(10_000_000L, 3.50, 12));
    }

    @Test
    void maturityInterest_10M_rate3_5_24months_isProRatedByMonths() {
        // 10,000,000 * 3.5% * 24/12 = 700,000
        assertEquals(700_000L, calculator.calculateInterest(10_000_000L, 3.50, 24));
    }

    @Test
    void earlyTerminationInterest_appliesLowerRateForElapsedMonths() {
        // 중도해지: 10,000,000 * 0.5% * 6/12 = 25,000
        assertEquals(25_000L, calculator.calculateInterest(10_000_000L, 0.50, 6));
    }

    @Test
    void interestIsZeroWhenNoMonthElapsed() {
        assertEquals(0L, calculator.calculateInterest(10_000_000L, 3.50, 0));
    }

    @Test
    void interestRoundsHalfUp() {
        // 1,000,000 * 3.33% * 7/12 = 19,425.0 → 19,425
        assertEquals(19_425L, calculator.calculateInterest(1_000_000L, 3.33, 7));
    }

    // ---------- calculateTax ----------

    @Test
    void tax_is15_4PercentOfInterest() {
        // 350,000 * 15.4% = 53,900
        assertEquals(53_900L, calculator.calculateTax(350_000L));
    }

    @Test
    void tax_isZeroWhenNoInterest() {
        assertEquals(0L, calculator.calculateTax(0L));
    }

    // ---------- elapsedMonths ----------

    @Test
    void elapsedMonths_countsWholeMonthsOnly() {
        LocalDate start = LocalDate.of(2026, 1, 15);
        assertEquals(0, calculator.elapsedMonths(start, LocalDate.of(2026, 2, 14)));
        assertEquals(1, calculator.elapsedMonths(start, LocalDate.of(2026, 2, 15)));
        assertEquals(6, calculator.elapsedMonths(start, LocalDate.of(2026, 7, 20)));
    }

    @Test
    void elapsedMonths_isZeroOnSameDay() {
        LocalDate start = LocalDate.of(2026, 1, 15);
        assertEquals(0, calculator.elapsedMonths(start, start));
    }
}
