package com.hanati.bank.bankEx.deposit.fixed.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class FixedDepositInterestCalculator {

    private static final double TAX_RATE = 0.154;

    /**
     * 단리 이자 = 예치 원금 × 연 이자율 × 개월 수 / 12.
     * 만기 계산 시 months = 가입 개월 수(TERM_MONTHS), 중도해지 계산 시 months = 경과 개월 수를 넘긴다.
     */
    public long calculateInterest(long principalAmount, double annualRatePercent, int months) {
        return Math.round(principalAmount * (annualRatePercent / 100) * months / 12.0);
    }

    public long calculateTax(long interest) {
        return Math.round(interest * TAX_RATE);
    }

    /** 예치 시작일부터 해지일까지 경과한 온전한 개월 수. */
    public int elapsedMonths(LocalDate startDate, LocalDate terminationDate) {
        return (int) ChronoUnit.MONTHS.between(startDate, terminationDate);
    }
}
