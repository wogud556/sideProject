package com.hanati.bank.mortgage.operator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 담당자 역할. 한 담당자는 한 역할만 가지므로 심사자와 승인자가 자연히 분리된다.
 */
@Getter
@RequiredArgsConstructor
public enum OperatorRole {
    OPERATIONS("여신 실무 (담보 평가·담보권 설정·대출 실행)"),
    UNDERWRITER("심사역 (한도 산정·여신 심사)"),
    APPROVER("승인권자 (대출 승인·정책 관리)");

    private final String description;
}
