package com.hanati.bank.mortgage.limit.engine;

import com.hanati.bank.mortgage.limit.enums.LimitConstraint;

import java.math.BigDecimal;

/**
 * 한도 산정 결과. 각 한도는 그 제약만 적용했을 때의 최대 원금이다.
 * requested* 비율은 신청 금액을 그대로 빌렸을 때의 값이다 (심사·화면 참고용).
 */
public record LimitCalculation(
        long ltvLimit,
        long collateralLimit,
        long dtiLimit,
        long dsrLimit,
        long stressDsrLimit,
        long productLimit,
        long regulatoryLimit,
        long finalLimit,
        LimitConstraint bindingConstraint,
        BigDecimal requestedLtv,
        BigDecimal requestedDti,
        BigDecimal requestedDsr,
        BigDecimal requestedStressDsr
) {
}
