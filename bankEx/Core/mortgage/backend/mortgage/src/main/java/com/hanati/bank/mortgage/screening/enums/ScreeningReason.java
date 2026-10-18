package com.hanati.bank.mortgage.screening.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScreeningReason {
    // 차주 심사
    LOW_CREDIT_SCORE("신용점수 600점 미만"),
    INCOME_DOCUMENT_MISSING("소득 증빙 서류 미확인"),
    // 담보 심사
    NO_COLLATERAL_CAPACITY("선순위 채권 차감 후 담보가용액 없음"),
    OWNERSHIP_DOCUMENT_MISSING("소유권·등기 서류 보완 필요 (실행 전 확인)"),
    // 규제 심사
    LIMIT_NOT_AVAILABLE("규제 한도 산정 결과 대출 가능 금액 없음"),
    REQUESTED_EXCEEDS_LIMIT("신청 금액이 산정 한도를 초과하여 한도 내 감액");

    private final String message;
}
