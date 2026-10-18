package com.hanati.bank.mortgage.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST("필수 입력값이 누락되었거나 올바르지 않습니다."),

    // 담당자
    OPERATOR_REQUIRED("담당자 ID(X-Operator-Id)가 필요합니다."),
    OPERATOR_NOT_FOUND("등록되지 않은 담당자입니다."),
    OPERATOR_NOT_AUTHORIZED("해당 업무를 처리할 권한이 없는 담당자입니다."),

    // 상품·신청
    PRODUCT_NOT_FOUND("주택담보대출 상품을 찾을 수 없습니다."),
    AMOUNT_OUT_OF_PRODUCT_RANGE("신청 금액이 상품의 대출 가능 범위를 벗어났습니다."),
    TERM_OUT_OF_PRODUCT_RANGE("대출 기간이 상품의 허용 범위를 벗어났습니다."),
    APPLICATION_NOT_FOUND("주택담보대출 신청 건을 찾을 수 없습니다."),
    APPLICATION_NOT_OWNED("본인의 신청 건이 아닙니다."),
    INVALID_STATUS_TRANSITION("현재 진행 단계에서는 처리할 수 없는 요청입니다."),

    // 담보·한도
    VALUATION_NOT_FOUND("담보 평가 이력이 없습니다. 담보 평가를 먼저 등록해 주세요."),
    LIMIT_RESULT_NOT_FOUND("한도 산정 결과가 없습니다. 한도를 먼저 산정해 주세요."),
    POLICY_NOT_FOUND("기준일에 적용할 규제 정책이 없습니다."),
    INVALID_POLICY_PERIOD("정책 적용 종료일은 시작일 이후여야 합니다."),

    // 심사·승인·약정
    SCREENING_NOT_FOUND("심사 결과가 없습니다."),
    INVALID_APPROVED_AMOUNT("승인 금액은 상품 최소 금액 이상, 심사 권고 금액 이하여야 합니다."),
    APPROVAL_NOT_FOUND("승인 정보가 없습니다."),
    APPROVAL_EXPIRED("승인 유효기간이 지났습니다."),
    AGREEMENT_NOT_FOUND("약정 정보가 없습니다."),
    AGREEMENT_CONSENT_REQUIRED("약정 내용에 동의해야 합니다."),

    // 실행·상환
    EXECUTION_PRECONDITION_NOT_MET("담보권 설정 또는 승인 조건 이행이 확인되지 않았습니다."),
    IDEMPOTENCY_KEY_CONFLICT("다른 요청에 이미 사용된 요청 키입니다."),
    ALREADY_EXECUTED("이미 실행된 대출입니다."),
    LOAN_ACCOUNT_NOT_FOUND("대출 계좌를 찾을 수 없습니다."),
    LOAN_ACCOUNT_NOT_OWNED("본인의 대출 계좌가 아닙니다."),
    LOAN_ACCOUNT_CLOSED("상환이 완료된 대출 계좌입니다."),
    REPAYMENT_AMOUNT_MISMATCH("상환 금액이 이번 회차 청구 금액과 일치하지 않습니다.");

    private final String message;
}
