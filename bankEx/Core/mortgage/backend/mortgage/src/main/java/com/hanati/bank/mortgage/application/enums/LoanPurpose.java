package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 대출 목적 */
@Getter
@RequiredArgsConstructor
public enum LoanPurpose {
    PURCHASE("주택 구입"),
    LIVING("생활안정");

    private final String label;
}
