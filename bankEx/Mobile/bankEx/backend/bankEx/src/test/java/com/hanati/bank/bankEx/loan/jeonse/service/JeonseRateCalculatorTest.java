package com.hanati.bank.bankEx.loan.jeonse.service;

import com.hanati.bank.bankEx.loan.jeonse.enums.GuaranteeOrg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeonseRateCalculatorTest {

    private final jeonseRateCalculator calculator = new jeonseRateCalculator();

    @Test
    void baseRatePlusAddOnsWithNoPreferentialDiscount() {
        double rate = calculator.calculate(820, 0.3077, GuaranteeOrg.HF, false, false, false);
        assertEquals(4.40, rate, 0.001);
    }

    @Test
    void preferentialRatesAreDeducted() {
        double rate = calculator.calculate(820, 0.3077, GuaranteeOrg.HF, true, true, false);
        assertEquals(4.10, rate, 0.001);
    }

    @Test
    void highCreditLowDebtSgiCombination() {
        double rate = calculator.calculate(950, 0.10, GuaranteeOrg.SGI, false, false, false);
        assertEquals(3.50 + 0.30 + 0.00 + 0.30, rate, 0.001);
    }
}
