package com.hanati.bank.mortgage.limit.engine;

import com.hanati.bank.mortgage.common.util.Money;
import com.hanati.bank.mortgage.limit.enums.LimitConstraint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 주담대 한도 산정 엔진. 승인 여부는 판단하지 않는다 — 결과는 심사 엔진의 입력값이다.
 *
 *   LTV 한도       = 담보평가액 × LTV
 *   담보가용액     = LTV 한도 − 선순위 채권·임차보증금
 *   DTI 한도       = (연소득 × DTI − 기존 주담대 원리금 − 기타 부채 이자) 를 원금으로 역산
 *   DSR 한도       = (연소득 × DSR − 기존 부채 원리금) 을 원금으로 역산
 *   스트레스 DSR   = DSR 한도를 (적용금리 + 스트레스 가산금리)로 다시 역산
 *   최종 한도      = min(담보가용액, DTI, 스트레스 DSR, 상품 한도, 규제 최대 금액)
 *
 * DSR은 스트레스 금리로 계산한 쪽이 항상 작으므로 최종 한도에는 스트레스 DSR만 들어간다.
 */
@Component
public class MortgageLimitEngine {

    public LimitCalculation calculate(LimitInput in) {
        BigDecimal factor = AnnualDebtService.factor(in.appliedRate(), in.termMonths(), in.repaymentMethod());
        BigDecimal stressFactor = AnnualDebtService.factor(in.appliedRate().add(in.stressRate()),
                in.termMonths(), in.repaymentMethod());

        long ltvLimit = Money.percentOfDown(in.collateralValue(), in.ltvPercent());
        long collateralLimit = Money.floorToTenThousand(BigDecimal.valueOf(ltvLimit - in.seniorClaims()));

        long dtiCapacity = Money.percentOfDown(in.annualIncome(), in.dtiPercent()) - in.existingDtiBurden();
        long dtiLimit = AnnualDebtService.principalFor(dtiCapacity, factor);

        long dsrCapacity = Money.percentOfDown(in.annualIncome(), in.dsrPercent()) - in.existingAnnualDebtService();
        long dsrLimit = AnnualDebtService.principalFor(dsrCapacity, factor);
        long stressDsrLimit = AnnualDebtService.principalFor(dsrCapacity, stressFactor);

        Map<LimitConstraint, Long> candidates = Map.of(
                LimitConstraint.COLLATERAL, collateralLimit,
                LimitConstraint.DTI, dtiLimit,
                LimitConstraint.STRESS_DSR, stressDsrLimit,
                LimitConstraint.PRODUCT, in.productMaxAmount(),
                LimitConstraint.REGULATORY, in.regulatoryMaxAmount());
        Map.Entry<LimitConstraint, Long> binding = candidates.entrySet().stream()
                .min(Map.Entry.<LimitConstraint, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .orElseThrow();

        long requestedPayment = AnnualDebtService.annualPayment(in.requestedAmount(), factor);
        long requestedStressPayment = AnnualDebtService.annualPayment(in.requestedAmount(), stressFactor);

        return new LimitCalculation(
                ltvLimit,
                collateralLimit,
                dtiLimit,
                dsrLimit,
                stressDsrLimit,
                in.productMaxAmount(),
                in.regulatoryMaxAmount(),
                binding.getValue(),
                binding.getKey(),
                BigDecimal.valueOf(in.requestedAmount() + in.seniorClaims()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(in.collateralValue()), 2, RoundingMode.HALF_UP),
                AnnualDebtService.ratioPercent(requestedPayment + in.existingDtiBurden(), in.annualIncome()),
                AnnualDebtService.ratioPercent(requestedPayment + in.existingAnnualDebtService(), in.annualIncome()),
                AnnualDebtService.ratioPercent(requestedStressPayment + in.existingAnnualDebtService(), in.annualIncome()));
    }
}
