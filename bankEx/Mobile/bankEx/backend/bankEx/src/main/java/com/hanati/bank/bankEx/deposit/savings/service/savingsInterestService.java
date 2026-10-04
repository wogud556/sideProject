package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.enums.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class savingsInterestService {

    private static final double TAX_RATE = 0.154;
    private static final double AUTO_TRANSFER_BONUS = 0.3;
    private static final double SALARY_TRANSFER_BONUS = 0.3;
    private static final double MOBILE_SIGNUP_BONUS = 0.2;
    private static final double LONG_TERM_BONUS = 0.2;

    public double calculateFinalRate(double baseRate, double maxRate, boolean autoTransferYn,
                                      boolean salaryTransferYn, boolean mobileSignupYn, boolean longTermYn) {
        double rate = baseRate;
        if (autoTransferYn) rate += AUTO_TRANSFER_BONUS;
        if (salaryTransferYn) rate += SALARY_TRANSFER_BONUS;
        if (mobileSignupYn) rate += MOBILE_SIGNUP_BONUS;
        if (longTermYn) rate += LONG_TERM_BONUS;
        double clamped = Math.min(rate, maxRate);
        return Math.round(clamped * 100.0) / 100.0;
    }

    /**
     * 회차별 납입액에 적립식 이자 공식(납입액 × 금리 × 잔여개월/12)을 적용해 합산한다.
     * 만기 계산 시 referenceCount = 전체 기간(PERIOD), 중도해지 계산 시 referenceCount = 해지 시점 납입회차(CURRENT_COUNT)를 넘긴다.
     */
    public long calculateInterest(List<SavingsPaymentHistory> payments, double appliedRatePercent, int referenceCount) {
        double rateFraction = appliedRatePercent / 100;
        long totalInterest = 0;
        for (SavingsPaymentHistory payment : payments) {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                continue;
            }
            int monthsRemaining = referenceCount - payment.getPaymentSeq() + 1;
            totalInterest += Math.round(payment.getPaymentAmount() * rateFraction * monthsRemaining / 12.0);
        }
        return totalInterest;
    }

    public double earlyTerminationRateRatio(int currentCount, int period) {
        double elapsedRatio = (double) currentCount / period;
        if (elapsedRatio < 0.30) return 0.30;
        if (elapsedRatio < 0.70) return 0.50;
        return 0.80;
    }

    public long calculateTax(long interest) {
        return Math.round(interest * TAX_RATE);
    }
}
