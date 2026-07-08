package com.hanati.bank.bankEx.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeonseLimitCalculatorTest {

    private final jeonseLimitCalculator calculator = new jeonseLimitCalculator();

    @Test
    void depositBasedLimitIsTheBindingConstraint() {
        long limit = calculator.calculate(130_000_000L, 65_000_000L, 20_000_000L, 222_000_000L);
        assertEquals(104_000_000L, limit);
    }

    @Test
    void productMaxLimitCapsResult() {
        long limit = calculator.calculate(1_000_000_000L, 500_000_000L, 0L, 222_000_000L);
        assertEquals(222_000_000L, limit);
    }

    @Test
    void debtDeductedLimitCanBeTheBindingConstraint() {
        long limit = calculator.calculate(500_000_000L, 30_000_000L, 90_000_000L, 222_000_000L);
        assertEquals(15_000_000L, limit);
    }

    @Test
    void negativeResultFloorsAtZero() {
        long limit = calculator.calculate(100_000_000L, 10_000_000L, 100_000_000L, 222_000_000L);
        assertEquals(0L, limit);
    }
}
