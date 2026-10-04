package com.hanati.bank.repayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 명세 8.5 응답. */
public record RepaymentTransactionResponse(
        String transactionNumber,
        Long loanAccountId,
        String transactionType,
        String paymentMethod,
        BigDecimal requestedAmount,
        BigDecimal allocatedAmount,
        BigDecimal overpaymentAmount,
        String status,
        LocalDate businessDate,
        LocalDateTime processedAt,
        String originalTransactionNumber,
        String reason,
        List<AllocationResponse> allocations
) {
}
