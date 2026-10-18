package com.hanati.bank.mortgage.repayment;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import com.hanati.bank.mortgage.repayment.calculator.RepaymentCalculator;
import com.hanati.bank.mortgage.repayment.calculator.ScheduleLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentCalculatorTest {

    private static final BigDecimal RATE = new BigDecimal("4.00");

    @ParameterizedTest
    @EnumSource(RepaymentMethod.class)
    @DisplayName("모든 상환 방식: 회차 수가 기간과 같고, 원금 합계가 대출금과 같고, 마지막 잔액은 0이다")
    void invariants(RepaymentMethod method) {
        List<ScheduleLine> lines = RepaymentCalculator.of(method).calculate(123_456_789L, RATE, 360);

        assertThat(lines).hasSize(360);
        assertThat(lines.stream().mapToLong(ScheduleLine::principal).sum()).isEqualTo(123_456_789L);
        assertThat(lines.get(359).balanceAfter()).isZero();
        assertThat(lines).allSatisfy(l -> assertThat(l.principal()).isNotNegative());
    }

    @Test
    @DisplayName("원리금균등: 1억·4%·30년은 매달 477,415원, 첫 회 이자 333,333원")
    void equalInstallment() {
        List<ScheduleLine> lines = RepaymentCalculator.of(RepaymentMethod.EQUAL_INSTALLMENT)
                .calculate(100_000_000L, RATE, 360);

        assertThat(lines.get(0).interest()).isEqualTo(333_333L);
        assertThat(lines.get(0).payment()).isEqualTo(477_415L);
        assertThat(lines.subList(0, 359)).allSatisfy(l -> assertThat(l.payment()).isEqualTo(477_415L));
        // 마지막 회차는 끝전 정리로 몇 원 차이 날 수 있다
        assertThat(lines.get(359).payment()).isBetween(477_000L, 478_000L);
    }

    @Test
    @DisplayName("원금균등: 매달 원금이 같고 이자는 줄어든다")
    void equalPrincipal() {
        List<ScheduleLine> lines = RepaymentCalculator.of(RepaymentMethod.EQUAL_PRINCIPAL)
                .calculate(120_000_000L, RATE, 120);

        assertThat(lines.subList(0, 120)).allSatisfy(l -> assertThat(l.principal()).isEqualTo(1_000_000L));
        assertThat(lines.get(0).interest()).isEqualTo(400_000L);
        assertThat(lines.get(1).interest()).isLessThan(lines.get(0).interest());
    }

    @Test
    @DisplayName("만기일시: 매달 이자만 내고 원금은 마지막 회차에 갚는다")
    void bullet() {
        List<ScheduleLine> lines = RepaymentCalculator.of(RepaymentMethod.BULLET).calculate(120_000_000L, RATE, 120);

        assertThat(lines).allSatisfy(l -> assertThat(l.interest()).isEqualTo(400_000L));
        assertThat(lines.subList(0, 119)).allSatisfy(l -> assertThat(l.principal()).isZero());
        assertThat(lines.get(119).principal()).isEqualTo(120_000_000L);
    }
}
