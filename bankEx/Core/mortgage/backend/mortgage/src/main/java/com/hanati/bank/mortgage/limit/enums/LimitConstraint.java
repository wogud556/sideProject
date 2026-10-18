package com.hanati.bank.mortgage.limit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 최종 한도를 결정한 제약. 여러 제약이 같은 금액이면 위에 있는 것을 기록한다. */
@Getter
@RequiredArgsConstructor
public enum LimitConstraint {
    COLLATERAL("담보가용액 (LTV − 선순위)"),
    DTI("DTI"),
    STRESS_DSR("스트레스 DSR"),
    PRODUCT("상품 한도"),
    REGULATORY("규제 최대 금액");

    private final String label;
}
