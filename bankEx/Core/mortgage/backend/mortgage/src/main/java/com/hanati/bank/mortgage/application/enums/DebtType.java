package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 기존 부채 유형. 신DTI는 주담대만 원리금, 나머지는 이자만 반영한다 */
@Getter
@RequiredArgsConstructor
public enum DebtType {
    MORTGAGE("주택담보대출"),
    CREDIT("신용대출"),
    JEONSE("전세자금대출"),
    OTHER("기타대출");

    private final String label;
}
