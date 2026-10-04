package com.hanati.bank.refinance.gateway.dto;

import java.math.BigDecimal;

public record RepaymentResult(
        boolean success,
        String transactionNo,
        BigDecimal repaidAmount,
        String resultCode,
        String resultMessage
) {
}
