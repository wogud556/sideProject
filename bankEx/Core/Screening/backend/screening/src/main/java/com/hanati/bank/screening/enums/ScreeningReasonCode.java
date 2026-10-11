package com.hanati.bank.screening.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScreeningReasonCode {
    BASIC_CRITERIA_MET("기본 심사 조건을 충족했습니다."),
    LOW_CREDIT_SCORE("신용점수가 기준(600점)에 미달합니다."),
    DEBT_LIMIT_EXCEEDED("기존 대출금과 신청 금액의 합계가 연소득의 10배를 초과합니다."),
    INCOME_MULTIPLE_EXCEEDED("신청 금액이 연소득의 500%를 초과하여 심사역 검토가 필요합니다."),
    BORDERLINE_CREDIT_SCORE("신용점수가 600~699점 구간이어서 심사역 검토가 필요합니다."),
    REVIEWER_APPROVED("심사역이 승인했습니다."),
    REVIEWER_REJECTED("심사역이 거절했습니다.");

    private final String message;
}
