package com.hanati.bank.refinance.gateway.dto;

import java.math.BigDecimal;

public record LoanExecutionResult(
        boolean success,
        String transactionNo,
        BigDecimal executedAmount,
        String resultCode,
        String resultMessage
) {
}
