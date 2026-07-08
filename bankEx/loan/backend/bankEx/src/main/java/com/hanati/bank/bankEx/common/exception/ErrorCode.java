package com.hanati.bank.bankEx.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CUSTOMER_NOT_FOUND("고객을 찾을 수 없습니다."),
    CUSTOMER_NOT_ACTIVE("정상 상태의 고객이 아닙니다."),
    DUPLICATED_USER_ID("이미 사용 중인 아이디입니다."),
    DUPLICATED_CUSTOMER("이미 등록된 고객입니다."),
    INVALID_REQUEST("필수 입력값이 누락되었습니다."),
    ACCOUNT_NOT_FOUND("계좌를 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE("정상 상태의 계좌가 아닙니다."),
    INSUFFICIENT_BALANCE("잔액이 부족합니다."),
    INVALID_AMOUNT("금액이 올바르지 않습니다."),
    LOAN_APPLICATION_NOT_FOUND("대출 신청 정보를 찾을 수 없습니다."),
    LOAN_NOT_APPROVED("승인되지 않은 대출입니다."),
    LOAN_ACCOUNT_MISMATCH("대출 계좌 정보가 일치하지 않습니다."),
    JEONSE_PRODUCT_NOT_FOUND("전세대출 상품을 찾을 수 없습니다."),
    JEONSE_APPLICATION_NOT_FOUND("전세대출 신청 정보를 찾을 수 없습니다."),
    JEONSE_INVALID_APPLICATION_STATE("현재 상태에서는 처리할 수 없는 요청입니다.");

    private final String message;
}
