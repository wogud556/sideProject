package com.hanati.bank.bankEx.loan.mortgage.dto;

/** Core가 보내는 실행금 입금 요청. idempotencyKey는 신청 건마다 하나다. */
public record MortgageDisbursementRequest(String idempotencyKey, String accountNumber, Long amount,
                                          String description) {
}
