package com.hanati.bank.mortgage.limit;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import com.hanati.bank.mortgage.limit.engine.AnnualDebtService;
import com.hanati.bank.mortgage.limit.engine.LimitCalculation;
import com.hanati.bank.mortgage.limit.engine.LimitInput;
import com.hanati.bank.mortgage.limit.engine.MortgageLimitEngine;
import com.hanati.bank.mortgage.limit.enums.LimitConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MortgageLimitEngineTest {

    private final MortgageLimitEngine engine = new MortgageLimitEngine();

    /** 명세 4.1 계산 예시: 담보 5억, LTV 70%, 연소득 6,500만, DSR 40%, 신청 3억, 4%, 30년 */
    private LimitInput.LimitInputBuilder spec() {
        return LimitInput.builder()
                .collateralValue(500_000_000L).seniorClaims(0L).annualIncome(65_000_000L)
                .existingAnnualDebtService(0L).existingDtiBurden(0L).requestedAmount(300_000_000L)
                .termMonths(360).repaymentMethod(RepaymentMethod.EQUAL_INSTALLMENT).appliedRate(new BigDecimal("4.00"))
                .ltvPercent(new BigDecimal("70")).dtiPercent(new BigDecimal("60")).dsrPercent(new BigDecimal("40"))
                .stressRate(new BigDecimal("1.50"))
                .regulatoryMaxAmount(1_000_000_000L).productMaxAmount(1_000_000_000L);
    }

    @Test
    @DisplayName("명세 예시: LTV 한도 3억 5천만 원이 최종 한도가 된다")
    void specExample() {
        LimitCalculation r = engine.calculate(spec().build());

        assertThat(r.ltvLimit()).isEqualTo(350_000_000L);
        assertThat(r.collateralLimit()).isEqualTo(350_000_000L);
        assertThat(r.finalLimit()).isEqualTo(350_000_000L);
        assertThat(r.bindingConstraint()).isEqualTo(LimitConstraint.COLLATERAL);
        // 연 2,600만 원 여력 ÷ 원금 1원당 연 원리금(4%, 30년) ≈ 4억 5천만 원대
        assertThat(r.dsrLimit()).isBetween(450_000_000L, 460_000_000L);
        assertThat(r.stressDsrLimit()).isLessThan(r.dsrLimit());
        assertThat(r.dsrLimit() % 10_000).isZero();
        assertThat(r.requestedLtv()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("원리금균등 1억·4%·30년의 월 상환액은 477,415원이다")
    void equalInstallmentFactor() {
        BigDecimal monthly = AnnualDebtService.monthlyInstallmentFactor(
                AnnualDebtService.monthlyRate(new BigDecimal("4.00")), 360);
        assertThat(AnnualDebtService.annualPayment(100_000_000L, monthly)).isEqualTo(477_415L);
    }

    @Test
    @DisplayName("선순위 채권은 LTV 한도에서 차감된다")
    void seniorClaimsReduceCollateralLimit() {
        LimitCalculation r = engine.calculate(spec().seniorClaims(100_000_000L).build());

        assertThat(r.ltvLimit()).isEqualTo(350_000_000L);
        assertThat(r.collateralLimit()).isEqualTo(250_000_000L);
        assertThat(r.finalLimit()).isEqualTo(250_000_000L);
        assertThat(r.requestedLtv()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("선순위가 LTV 한도를 넘으면 담보가용액은 0이다")
    void seniorClaimsExceedLtv() {
        LimitCalculation r = engine.calculate(spec().seniorClaims(400_000_000L).build());

        assertThat(r.collateralLimit()).isZero();
        assertThat(r.finalLimit()).isZero();
    }

    @Test
    @DisplayName("기존 부채 원리금이 DSR 여력을 줄이면 스트레스 DSR이 최종 한도를 정한다")
    void stressDsrBinds() {
        LimitCalculation r = engine.calculate(spec().annualIncome(30_000_000L)
                .existingAnnualDebtService(8_000_000L).existingDtiBurden(1_000_000L).build());

        // 여력 = 3,000만 × 40% − 800만 = 400만
        assertThat(r.bindingConstraint()).isEqualTo(LimitConstraint.STRESS_DSR);
        assertThat(r.finalLimit()).isEqualTo(r.stressDsrLimit());
        assertThat(r.stressDsrLimit()).isBetween(50_000_000L, 60_000_000L);
        assertThat(r.requestedDsr()).isGreaterThan(new BigDecimal("40"));
    }

    @Test
    @DisplayName("기존 부채가 DSR 여력을 넘으면 DSR 한도는 0이다")
    void noDsrCapacity() {
        LimitCalculation r = engine.calculate(spec().existingAnnualDebtService(30_000_000L).build());

        assertThat(r.dsrLimit()).isZero();
        assertThat(r.stressDsrLimit()).isZero();
        assertThat(r.finalLimit()).isZero();
    }

    @Test
    @DisplayName("DTI 비율이 DSR보다 낮으면 DTI가 최종 한도를 정한다")
    void dtiBinds() {
        LimitCalculation r = engine.calculate(spec().dtiPercent(new BigDecimal("25")).build());

        assertThat(r.bindingConstraint()).isEqualTo(LimitConstraint.DTI);
        assertThat(r.finalLimit()).isEqualTo(r.dtiLimit());
    }

    @Test
    @DisplayName("규제 최대 금액·상품 한도도 최종 한도 후보다")
    void regulatoryAndProductLimits() {
        LimitCalculation regulatory = engine.calculate(spec().regulatoryMaxAmount(200_000_000L).build());
        assertThat(regulatory.bindingConstraint()).isEqualTo(LimitConstraint.REGULATORY);
        assertThat(regulatory.finalLimit()).isEqualTo(200_000_000L);

        LimitCalculation product = engine.calculate(spec().productMaxAmount(100_000_000L).build());
        assertThat(product.bindingConstraint()).isEqualTo(LimitConstraint.PRODUCT);
        assertThat(product.finalLimit()).isEqualTo(100_000_000L);
    }

    @Test
    @DisplayName("만기일시는 DSR 산정 시 원금을 대출기간으로 나눠 반영한다")
    void bulletFactor() {
        BigDecimal factor = AnnualDebtService.factor(new BigDecimal("4.00"), 120, RepaymentMethod.BULLET);
        // 연 이자 4% + 원금 1/10
        assertThat(AnnualDebtService.annualPayment(100_000_000L, factor)).isEqualTo(14_000_000L);
    }
}
