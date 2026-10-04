package com.hanati.bank.refinance.refinance.calculator;

import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 원금/이자/수수료 조합이 명세 9번 공식(원금잔액 + 발생이자 + 중도상환수수료 + 기타비용 - 감면금액)대로
 * BigDecimal로 정확히 계산되는지 검증한다. 날짜는 윤년을 피해 일수를 손으로 검증 가능하게 골랐다:
 * 실행일 2025-04-10 ~ 만기일 2027-04-10(총 730일), 기준일 2026-04-10(잔여 365일, 잔여비율 정확히 0.5).
 */
class RepaymentAmountCalculatorTest {

    private final RepaymentAmountCalculator calculator = new RepaymentAmountCalculator();

    @Test
    void calculates_accrued_interest_prepayment_fee_and_final_amount_precisely() {
        ExistingLoan loan = ExistingLoan.builder()
                .currentBalance(new BigDecimal("10000000"))
                .interestRate(new BigDecimal("7.30")) // 10,000,000 * 0.073 * 10/365 = 20,000 정확히 나누어떨어짐
                .executionDate(LocalDate.of(2025, 4, 10))
                .maturityDate(LocalDate.of(2027, 4, 10)) // 총 730일
                .build();
        LocalDate baseDate = LocalDate.of(2026, 4, 10); // 잔여 365일 -> 잔여비율 0.5

        RepaymentAmountResult result = calculator.calculate(loan, baseDate);

        assertEquals(new BigDecimal("10000000"), result.principalBalance());
        assertEquals(0, new BigDecimal("20000").compareTo(result.accruedInterest()));
        assertEquals(0, new BigDecimal("70000").compareTo(result.prepaymentFee())); // 10,000,000 * 0.014 * 0.5
        assertEquals(0, BigDecimal.ZERO.compareTo(result.otherCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.discountAmount()));
        assertEquals(0, new BigDecimal("10090000").compareTo(result.finalRepaymentAmount()));
    }

    @Test
    void final_amount_is_never_negative_when_maturity_already_passed() {
        ExistingLoan loan = ExistingLoan.builder()
                .currentBalance(new BigDecimal("5000000"))
                .interestRate(new BigDecimal("5.00"))
                .executionDate(LocalDate.of(2020, 1, 1))
                .maturityDate(LocalDate.of(2021, 1, 1))
                .build();
        LocalDate baseDate = LocalDate.of(2026, 1, 1); // 이미 만기 경과 -> 중도상환수수료는 0

        RepaymentAmountResult result = calculator.calculate(loan, baseDate);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.prepaymentFee()));
    }
}
