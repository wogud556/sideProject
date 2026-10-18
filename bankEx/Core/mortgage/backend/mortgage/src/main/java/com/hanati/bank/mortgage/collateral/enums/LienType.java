package com.hanati.bank.mortgage.collateral.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 담보 주택의 권리관계. OWN_MORTGAGE를 뺀 나머지가 담보가용액에서 차감되는 선순위다. */
@Getter
@RequiredArgsConstructor
public enum LienType {
    SENIOR_MORTGAGE("선순위 근저당"),
    LEASE_DEPOSIT("임차보증금"),
    OWN_MORTGAGE("당행 근저당");

    private final String label;

    public boolean isSeniorClaim() {
        return this != OWN_MORTGAGE;
    }
}
