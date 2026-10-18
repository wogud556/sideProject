package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 차주 유형. 신청 시 보유 주택 수와 생애최초 여부로 정해진다 */
@Getter
@RequiredArgsConstructor
public enum BorrowerType {
    FIRST_HOME("생애최초 구입자"),
    NO_HOME("무주택자"),
    ONE_HOME("1주택자"),
    MULTI_HOME("다주택자");

    private final String label;
}
