package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 상환 방식 */
@Getter
@RequiredArgsConstructor
public enum RepaymentMethod {
    EQUAL_INSTALLMENT("원리금균등"),
    EQUAL_PRINCIPAL("원금균등"),
    BULLET("만기일시");

    private final String label;
}
