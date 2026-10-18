package com.hanati.bank.mortgage.execution.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoanAccountStatus {
    ACTIVE("정상"),
    CLOSED("완제");

    private final String label;
}
