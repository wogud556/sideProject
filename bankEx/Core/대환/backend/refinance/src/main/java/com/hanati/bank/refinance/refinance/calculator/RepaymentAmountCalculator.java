package com.hanati.bank.refinance.refinance.calculator;

import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 상환예정금액 = 원금잔액 + 당일까지 발생이자 + 중도상환수수료 + 기타비용 - 감면금액 (명세 9번).
 * 모든 금융 금액 계산은 BigDecimal로 수행한다 (명세 37번 원칙 2).
 *
 * - 발생이자: 이번 달 1일부터 기준일까지 경과일수 기준 단리 계산 (payment schedule을 별도로 관리하지 않는 Mock 환경의 간이 산정).
 * - 중도상환수수료: 잔여기간/전체기간에 비례하는 국내 시중은행 일반적 산정 방식을 간이화 (요율 1.4% 고정) — 실제 금융기관 고지 수수료율과는 다를 수 있음.
 */
@Component
public class RepaymentAmountCalculator {

    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.014");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    public RepaymentAmountResult calculate(ExistingLoan loan, LocalDate baseDate) {
        BigDecimal principal = loan.getCurrentBalance();

        BigDecimal accruedInterest = calculateAccruedInterest(principal, loan.getInterestRate(), baseDate);
        BigDecimal prepaymentFee = calculatePrepaymentFee(principal, loan.getExecutionDate(), loan.getMaturityDate(), baseDate);
        BigDecimal otherCost = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        BigDecimal finalAmount = principal
                .add(accruedInterest)
                .add(prepaymentFee)
                .add(otherCost)
                .subtract(discountAmount)
                .setScale(0, RoundingMode.HALF_UP);

        return new RepaymentAmountResult(
                loan.getLoanId(),
                principal,
                accruedInterest,
                prepaymentFee,
                otherCost,
                discountAmount,
                finalAmount,
                LocalDateTime.now()
        );
    }

    private BigDecimal calculateAccruedInterest(BigDecimal principal, BigDecimal annualRatePercent, LocalDate baseDate) {
        int daysElapsedThisMonth = baseDate.getDayOfMonth();
        BigDecimal rate = annualRatePercent.divide(HUNDRED, 10, RoundingMode.HALF_UP);
        return principal.multiply(rate)
                .multiply(BigDecimal.valueOf(daysElapsedThisMonth))
                .divide(DAYS_IN_YEAR, 0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePrepaymentFee(BigDecimal principal, LocalDate executionDate, LocalDate maturityDate, LocalDate baseDate) {
        long totalDays = ChronoUnit.DAYS.between(executionDate, maturityDate);
        long remainingDays = ChronoUnit.DAYS.between(baseDate, maturityDate);
        if (totalDays <= 0 || remainingDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainingRatio = BigDecimal.valueOf(Math.min(remainingDays, totalDays))
                .divide(BigDecimal.valueOf(totalDays), 10, RoundingMode.HALF_UP);
        return principal.multiply(PREPAYMENT_FEE_RATE)
                .multiply(remainingRatio)
                .setScale(0, RoundingMode.HALF_UP);
    }
}
