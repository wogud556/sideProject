package com.hanati.bank.mortgage.limit.engine;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import com.hanati.bank.mortgage.common.util.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 연간 원리금 상환액 계산. DTI·DSR 비율과 한도 역산에 함께 쓴다.
 *
 * 상환액은 원금에 비례하므로 "원금 1원당 연간 원리금(factor)"을 먼저 구한다.
 *   연간 원리금 = 원금 × factor
 *   한도 원금  = 연간 상환 여력 ÷ factor
 * 연간 원리금은 첫 1년(최대 12회차) 기준이다. 만기일시는 원금을 대출기간으로 나눠 매년 갚는 것으로 본다.
 */
public final class AnnualDebtService {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AnnualDebtService() {
    }

    public static BigDecimal factor(BigDecimal annualRatePercent, int termMonths, RepaymentMethod method) {
        BigDecimal r = monthlyRate(annualRatePercent);
        BigDecimal n = BigDecimal.valueOf(termMonths);
        int firstYearMonths = Math.min(12, termMonths);

        return switch (method) {
            case EQUAL_INSTALLMENT -> monthlyInstallmentFactor(r, termMonths).multiply(BigDecimal.valueOf(firstYearMonths), MC);
            case EQUAL_PRINCIPAL -> {
                // k회차 상환액 = 1/n + r × (1 − k/n), k = 0..11
                BigDecimal sum = BigDecimal.ZERO;
                for (int k = 0; k < firstYearMonths; k++) {
                    BigDecimal remaining = BigDecimal.ONE.subtract(BigDecimal.valueOf(k).divide(n, MC));
                    sum = sum.add(BigDecimal.ONE.divide(n, MC)).add(r.multiply(remaining, MC));
                }
                yield sum;
            }
            case BULLET -> r.multiply(BigDecimal.valueOf(firstYearMonths), MC)
                    .add(BigDecimal.valueOf(firstYearMonths).divide(n, MC));
        };
    }

    /** 원리금균등 월 상환액 / 원금 = r / (1 − (1+r)^−n) */
    public static BigDecimal monthlyInstallmentFactor(BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return BigDecimal.ONE.divide(BigDecimal.valueOf(termMonths), MC);
        }
        BigDecimal discount = BigDecimal.ONE.divide(BigDecimal.ONE.add(monthlyRate).pow(termMonths, MC), MC);
        return monthlyRate.divide(BigDecimal.ONE.subtract(discount), MC);
    }

    public static BigDecimal monthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent.divide(HUNDRED, MC).divide(TWELVE, MC);
    }

    public static long annualPayment(long principal, BigDecimal factor) {
        return Money.roundWon(BigDecimal.valueOf(principal).multiply(factor, MC));
    }

    /** 연간 상환 여력으로 갚을 수 있는 최대 원금. 만원 미만 버림, 여력이 없으면 0 */
    public static long principalFor(long annualCapacity, BigDecimal factor) {
        if (annualCapacity <= 0) {
            return 0L;
        }
        return Money.floorToTenThousand(BigDecimal.valueOf(annualCapacity).divide(factor, MC));
    }

    /** (분자 ÷ 연소득) × 100, 소수 둘째 자리 반올림 */
    public static BigDecimal ratioPercent(long numerator, long annualIncome) {
        return BigDecimal.valueOf(numerator).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(annualIncome), 2, RoundingMode.HALF_UP);
    }
}
