package com.hanati.bank.mortgage.policy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyType {
    LTV("LTV 한도 (%)"),
    DTI("DTI 한도 (%)"),
    DSR("DSR 한도 (%)"),
    STRESS_RATE("스트레스 가산금리 (%p)"),
    MAX_LOAN_AMOUNT("규제 최대 대출금액 (원)");

    private final String label;
}
