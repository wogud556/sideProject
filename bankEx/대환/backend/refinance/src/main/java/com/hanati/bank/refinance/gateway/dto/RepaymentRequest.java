package com.hanati.bank.refinance.gateway.dto;

import java.math.BigDecimal;

public record RepaymentRequest(
        String requestId,
        Long applicationId,
        Long loanId,
        String financialInstitutionCode,
        String loanAccountNo,
        BigDecimal amount
) {
}
