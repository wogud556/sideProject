package com.hanati.bank.screening.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST("필수 입력값이 누락되었거나 올바르지 않습니다."),
    SCREENING_NOT_FOUND("심사 내역을 찾을 수 없습니다."),
    SCREENING_NOT_MANUAL_REVIEW("수동심사 대기 건만 심사역이 결정할 수 있습니다."),
    INVALID_APPROVED_AMOUNT("승인 금액은 0원보다 크고 신청 금액 이하여야 합니다.");

    private final String message;
}
