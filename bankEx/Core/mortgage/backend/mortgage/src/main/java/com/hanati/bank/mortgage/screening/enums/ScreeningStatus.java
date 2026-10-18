package com.hanati.bank.mortgage.screening.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 심사 결과 (명세 6). 심사는 요청 즉시 끝나므로 명세의 PENDING·IN_PROGRESS는 두지 않는다.
 * 조건부 승인도 실행 가능 상태가 아니다 — 승인·약정·조건 이행을 거쳐야 실행할 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum ScreeningStatus {
    APPROVED("승인"),
    CONDITIONALLY_APPROVED("조건부 승인"),
    ADDITIONAL_DOCUMENT_REQUIRED("추가 서류 요청"),
    REJECTED("거절");

    private final String label;
}
