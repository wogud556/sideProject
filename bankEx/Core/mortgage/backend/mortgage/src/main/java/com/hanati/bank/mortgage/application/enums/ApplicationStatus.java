package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 주담대 신청 진행 상태. 전이 규칙은 ApplicationStatusTransition에 모은다 */
@Getter
@RequiredArgsConstructor
public enum ApplicationStatus {
    APPLIED("신청 접수"),
    COLLATERAL_EVALUATED("담보 평가 완료"),
    LIMIT_CALCULATED("한도 산정 완료"),
    DOCUMENT_REQUIRED("추가 서류 요청"),
    SCREENING_APPROVED("심사 승인"),
    CONDITIONALLY_APPROVED("조건부 심사 승인"),
    REJECTED("거절"),
    APPROVED("대출 승인"),
    AGREED("약정 완료"),
    EXECUTION_READY("실행 대기"),
    EXECUTED("실행 완료"),
    WITHDRAWN("신청 철회");

    private final String label;
}
