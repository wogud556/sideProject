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
    ACCOUNT_ALREADY_CLOSED("이미 해지된 계좌입니다."),
    ACCOUNT_BALANCE_NOT_ZERO("계좌 잔액이 0원이 아니어서 해지할 수 없습니다."),
    ACCOUNT_HAS_ACTIVE_LOAN("해당 계좌에 진행 중인 대출이 있어 해지할 수 없습니다."),
    INSUFFICIENT_BALANCE("잔액이 부족합니다."),
    INVALID_AMOUNT("금액이 올바르지 않습니다."),
    LOAN_APPLICATION_NOT_FOUND("대출 신청 정보를 찾을 수 없습니다."),
    LOAN_NOT_APPROVED("승인되지 않은 대출입니다."),
    LOAN_ACCOUNT_MISMATCH("대출 계좌 정보가 일치하지 않습니다."),
    LOAN_ALREADY_COMPLETED("이미 상환 완료된 대출입니다."),
    LOAN_NOT_ACTIVE("실행 완료 상태의 대출이 아닙니다."),
    LOAN_PRODUCT_NOT_FOUND("대출 상품 정보를 찾을 수 없습니다."),
    LOAN_INVALID_REPAYMENT_TYPE("유효하지 않은 상환 방식입니다."),
    LOAN_INTEREST_RATE_INVALID("대출 상품의 이자율이 유효하지 않습니다."),
    JEONSE_PRODUCT_NOT_FOUND("전세대출 상품을 찾을 수 없습니다."),
    JEONSE_APPLICATION_NOT_FOUND("전세대출 신청 정보를 찾을 수 없습니다."),
    JEONSE_INVALID_APPLICATION_STATE("현재 상태에서는 처리할 수 없는 요청입니다."),
    SAVINGS_PRODUCT_NOT_FOUND("적금 상품을 찾을 수 없습니다."),
    SAVINGS_PRODUCT_CLOSED("판매가 종료된 상품입니다."),
    SAVINGS_ACCOUNT_NOT_FOUND("적금 계좌를 찾을 수 없습니다."),
    SAVINGS_AMOUNT_OUT_OF_RANGE("가입 금액이 상품의 허용 범위를 벗어났습니다."),
    SAVINGS_INVALID_PERIOD("가입 기간이 상품 조건과 일치하지 않습니다."),
    SAVINGS_DUPLICATED_SUBSCRIPTION("이미 가입 중인 적금 계좌가 있습니다."),
    SAVINGS_INVALID_TRANSFER_DAY("자동이체일은 5, 10, 15, 20, 25일 중 하나여야 합니다."),
    SAVINGS_ALREADY_TERMINATED("이미 해지되었거나 만기 처리된 계좌입니다."),
    SAVINGS_FULLY_PAID("이미 전체 회차 납입이 완료되었습니다."),
    ACCOUNT_NOT_OWNED("본인 소유의 출금계좌가 아닙니다."),
    SAME_ACCOUNT_TRANSFER("동일 계좌 간 이체는 불가능합니다."),
    INVALID_ACCOUNT_PASSWORD("계좌 비밀번호가 일치하지 않습니다."),
    INVALID_TRANSFER_AMOUNT("이체 금액이 올바르지 않습니다."),
    PER_TRANSFER_LIMIT_EXCEEDED("1회 이체한도를 초과했습니다."),
    DAILY_TRANSFER_LIMIT_EXCEEDED("1일 이체한도를 초과했습니다."),
    DUPLICATE_TRANSFER_REQUEST("이미 처리 중이거나 완료된 이체 요청입니다."),
    TRANSFER_NOT_FOUND("이체 내역을 찾을 수 없습니다."),
    TRANSFER_PROCESSING_FAILED("이체 처리 중 오류가 발생했습니다.");

    private final String message;
}
