package com.hanati.bank.mortgage.repayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 고객 채널(bankEx)이 출금을 마친 뒤 보낸다.
 * amount는 이번 회차 청구액과 같아야 하며, idempotencyKey는 bankEx 출금 거래번호다.
 */
public record RepaymentRequest(
        @NotBlank String customerId,
        @NotNull @Positive Long amount,
        @NotBlank String sourceAccountNo,
        @NotBlank String idempotencyKey
) {
}
