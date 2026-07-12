package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavingsInterestServiceTest {

    private final savingsInterestService service = new savingsInterestService();

    @Test
    void finalRateSumsAllPreferentialBonuses() {
        double rate = service.calculateFinalRate(2.80, 10.00, true, true, true, true);
        assertEquals(2.80 + 0.3 + 0.3 + 0.2 + 0.2, rate, 0.001);
    }

    @Test
    void finalRateClampsToProductMaxRate() {
        double rate = service.calculateFinalRate(2.80, 3.00, true, true, true, true);
        assertEquals(3.00, rate, 0.001);
    }

    @Test
    void finalRateWithNoBonuses() {
        double rate = service.calculateFinalRate(2.80, 4.00, false, false, false, false);
        assertEquals(2.80, rate, 0.001);
    }

    @Test
    void maturityInterestSumsPerPaymentRemainingMonths() {
        List<SavingsPaymentHistory> payments = List.of(
                payment(1, 100_000L),
                payment(2, 100_000L),
                payment(3, 100_000L)
        );
        long interest = service.calculateInterest(payments, 12.0, 3);
        long expected = Math.round(100_000 * 0.12 * 3 / 12.0)
                + Math.round(100_000 * 0.12 * 2 / 12.0)
                + Math.round(100_000 * 0.12 * 1 / 12.0);
        assertEquals(expected, interest);
    }

    @Test
    void failedPaymentsAreExcludedFromInterest() {
        List<SavingsPaymentHistory> payments = List.of(
                payment(1, 100_000L),
                SavingsPaymentHistory.builder().paymentSeq(2).paymentAmount(100_000L).status(PaymentStatus.FAILED).build()
        );
        long interest = service.calculateInterest(payments, 12.0, 2);
        assertEquals(Math.round(100_000 * 0.12 * 2 / 12.0), interest);
    }

    @Test
    void earlyTerminationRatioTiers() {
        assertEquals(0.30, service.earlyTerminationRateRatio(2, 12), 0.001);
        assertEquals(0.50, service.earlyTerminationRateRatio(5, 12), 0.001);
        assertEquals(0.80, service.earlyTerminationRateRatio(10, 12), 0.001);
    }

    @Test
    void taxIsAppliedAtFixedRate() {
        long tax = service.calculateTax(100_000);
        assertEquals(Math.round(100_000 * 0.154), tax);
    }

    private SavingsPaymentHistory payment(int seq, long amount) {
        return SavingsPaymentHistory.builder()
                .paymentSeq(seq)
                .paymentDate(LocalDate.now())
                .paymentAmount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();
    }
}
