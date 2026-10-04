package com.hanati.bank.repayment.dto;

import java.math.BigDecimal;
import java.util.List;

/** 명세 8.2 응답. */
public record RepaymentResponse(
        String transactionNumber,
        Long loanAccountId,
        String transactionType,
        BigDecimal requestedAmount,
        BigDecimal allocatedAmount,
        BigDecimal overpaymentAmount,
        String status,
        List<AllocationResponse> allocations,
        BigDecimal remainingPrincipal,
        BigDecimal totalOutstanding,
        String loanAccountStatus,
        Long overpaymentId
) {
}
