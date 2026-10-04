package com.hanati.bank.refinance.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CUSTOMER_NOT_FOUND("고객을 찾을 수 없습니다."),
    LOAN_NOT_FOUND("대출을 찾을 수 없습니다."),
    LOAN_NOT_OWNED_BY_CUSTOMER("해당 고객의 대출이 아닙니다."),
    REFINANCE_NOT_ELIGIBLE("대환 대상이 아닙니다."),
    OVERDUE_LOAN("연체 중인 대출은 대환할 수 없습니다."),
    APPLICATION_NOT_FOUND("대환 신청 정보를 찾을 수 없습니다."),
    INVALID_APPLICATION_STATUS("현재 상태에서는 처리할 수 없는 요청입니다."),
    APPLICATION_ALREADY_EXISTS("이미 진행 중인 대환 신청이 존재합니다."),
    APPROVAL_REQUIRED("승인이 필요합니다."),
    LOAN_EXECUTION_FAILED("신규대출 실행에 실패했습니다."),
    REPAYMENT_FAILED("기존대출 상환에 실패했습니다."),
    DUPLICATE_TRANSACTION("이미 처리된 거래입니다."),
    CONCURRENT_MODIFICATION("다른 요청에 의해 이미 처리되었습니다. 다시 조회해 주세요."),
    OPERATOR_NOT_FOUND("직원 정보를 찾을 수 없습니다."),
    FORBIDDEN_ROLE("해당 작업을 수행할 권한이 없습니다."),
    INVALID_REQUEST("필수 입력값이 누락되었거나 올바르지 않습니다."),
    INVALID_AMOUNT("금액이 올바르지 않습니다."),
    ERROR_RECORD_NOT_FOUND("재처리 대상 오류 이력을 찾을 수 없습니다."),
    NOT_RETRYABLE("현재 상태에서는 재처리할 수 없습니다.");

    private final String message;
}
