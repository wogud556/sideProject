package com.hanati.bank.repayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 명세 8.1 응답. */
public record RepaymentQuoteResponse(
        Long loanAccountId,
        LocalDate quoteDate,
        String repaymentType,
        BigDecimal principal,
        BigDecimal overduePrincipal,
        BigDecimal interest,
        BigDecimal overdueInterest,
        BigDecimal fee,
        BigDecimal totalAmount,
        LocalDateTime validUntil
) {
}
