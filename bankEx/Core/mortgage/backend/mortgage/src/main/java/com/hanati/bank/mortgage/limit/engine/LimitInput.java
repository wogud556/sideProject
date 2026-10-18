package com.hanati.bank.mortgage.limit.engine;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 한도 산정 입력. 정책 값은 서비스가 PolicyResolver로 찾아 넣는다 (엔진은 DB를 모른다).
 *
 * @param existingAnnualDebtService 기존 부채 전체의 연간 원리금 (DSR 분자)
 * @param existingDtiBurden         기존 주담대 연간 원리금 + 기타 부채 연간 이자 (신DTI 분자)
 */
@Builder
public record LimitInput(
        long collateralValue,
        long seniorClaims,
        long annualIncome,
        long existingAnnualDebtService,
        long existingDtiBurden,
        long requestedAmount,
        int termMonths,
        RepaymentMethod repaymentMethod,
        BigDecimal appliedRate,
        BigDecimal ltvPercent,
        BigDecimal dtiPercent,
        BigDecimal dsrPercent,
        BigDecimal stressRate,
        long regulatoryMaxAmount,
        long productMaxAmount
) {
}
