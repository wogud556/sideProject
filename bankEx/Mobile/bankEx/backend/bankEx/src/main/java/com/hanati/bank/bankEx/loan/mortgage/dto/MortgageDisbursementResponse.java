package com.hanati.bank.bankEx.loan.mortgage.dto;

/** duplicate가 true면 이미 처리된 키라 새로 입금하지 않고 처음 거래를 돌려준 것이다. */
public record MortgageDisbursementResponse(String transactionNumber, String accountNumber, Long amount,
                                           Long balanceAfter, boolean duplicate) {
}
