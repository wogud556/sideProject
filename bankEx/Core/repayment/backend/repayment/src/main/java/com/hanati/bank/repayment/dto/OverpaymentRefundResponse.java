package com.hanati.bank.repayment.dto;

import java.math.BigDecimal;

/** 명세 8.6 응답. */
public record OverpaymentRefundResponse(
        Long overpaymentId,
        Long loanAccountId,
        BigDecimal amount,
        String status,
        String refundTransactionNumber,
        String message
) {
}
