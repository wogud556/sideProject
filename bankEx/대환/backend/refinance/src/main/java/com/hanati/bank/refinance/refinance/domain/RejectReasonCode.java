package com.hanati.bank.refinance.refinance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RejectReasonCode {
    OVERDUE_LOAN("연체 중인 대출은 대환할 수 없습니다."),
    NO_LOAN_BALANCE("대출 잔액이 없어 대환할 수 없습니다."),
    ALREADY_IN_PROGRESS("이미 진행 중인 대환 신청이 존재합니다."),
    NOT_TARGET_PRODUCT("대환 대상 상품이 아닙니다."),
    CUSTOMER_NOT_ACTIVE("정상 상태의 고객이 아닙니다."),
    LIMIT_EXCEEDED("신규대출 가능 한도를 초과했습니다."),
    INVALID_REQUEST_AMOUNT("대환 신청 금액이 적정하지 않습니다."),
    LOW_CREDIT("심사 기준에 미달합니다."),
    HIGH_DEBT_RATIO("부채비율이 기준을 초과합니다.");

    private final String message;
}
