package com.hanati.bank.repayment.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 명세 12번. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    LOAN_ACCOUNT_NOT_FOUND("대출계좌를 찾을 수 없습니다."),
    INVALID_LOAN_STATUS("현재 대출 상태에서는 상환할 수 없습니다."),
    INVALID_REPAYMENT_AMOUNT("상환 금액이 올바르지 않습니다."),
    DUPLICATE_REPAYMENT_REQUEST("동일한 멱등성 키로 다른 내용의 요청이 접수되었습니다."),
    REPAYMENT_TRANSACTION_NOT_FOUND("상환 거래를 찾을 수 없습니다."),
    REPAYMENT_ALREADY_REVERSED("이미 취소된 상환 거래입니다."),
    REPAYMENT_REVERSAL_NOT_ALLOWED("해당 상환 거래는 취소할 수 없습니다."),
    REPAYMENT_ALLOCATION_FAILED("상환금 배분 결과가 정합하지 않습니다."),
    INSUFFICIENT_WITHDRAWAL_BALANCE("출금계좌 잔액이 부족합니다."),
    REPAYMENT_CONCURRENCY_CONFLICT("다른 상환 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요."),
    FULL_REPAYMENT_AMOUNT_CHANGED("전액 상환 예정금액이 변경되었습니다. 다시 조회해 주세요."),
    OVERPAYMENT_NOT_FOUND("과오납 내역을 찾을 수 없습니다."),
    OVERPAYMENT_ALREADY_REFUNDED("이미 환급 처리된 과오납입니다."),
    INVALID_REQUEST("필수 입력값이 누락되었습니다."),
    INVALID_PAYMENT_DATE("상환일이 올바르지 않습니다."),
    WITHDRAWAL_ACCOUNT_NOT_REGISTERED("자동이체 출금계좌가 등록되지 않았습니다."),
    WITHDRAWAL_FAILED("출금 처리에 실패했습니다.");

    private final String message;
}
