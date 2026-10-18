package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 소득 유형 */
@Getter
@RequiredArgsConstructor
public enum IncomeType {
    EMPLOYEE("근로소득"),
    BUSINESS("사업소득"),
    OTHER("기타소득");

    private final String label;
}
