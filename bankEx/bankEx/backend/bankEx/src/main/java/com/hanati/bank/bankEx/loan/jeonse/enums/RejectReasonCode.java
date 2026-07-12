package com.hanati.bank.bankEx.loan.jeonse.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RejectReasonCode {
    NOT_LOGIN("로그인이 필요합니다."),
    NOT_HOUSEHOLDER("세대주가 아니어서 신청할 수 없습니다."),
    NOT_HOMELESS("무주택자가 아니어서 신청할 수 없습니다."),
    DEPOSIT_LIMIT_EXCEEDED("전세보증금이 한도를 초과했습니다."),
    DOWN_PAYMENT_SHORTAGE("계약금이 전세보증금의 5% 미만입니다."),
    INVALID_CONTRACT_DATE("계약 종료일이 시작일보다 이후여야 합니다."),
    LOW_CREDIT_SCORE("신용점수가 기준에 미달합니다."),
    HIGH_DEBT_RATIO("부채비율이 기준을 초과했습니다."),
    LIMIT_SHORTAGE("신청 금액이 산출된 한도를 초과했습니다."),
    NO_FIXED_DATE("확정일자가 없어 심사가 보류되었습니다."),
    GUARANTEE_REJECTED("보증기관 심사에서 거절되었습니다."),
    BANK_REJECTED("은행 내부 심사에서 거절되었습니다.");

    private final String message;
}
