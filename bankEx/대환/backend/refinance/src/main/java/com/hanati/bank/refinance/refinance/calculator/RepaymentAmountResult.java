package com.hanati.bank.refinance.refinance.calculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RepaymentAmountResult(
        Long loanId,
        BigDecimal principalBalance,
        BigDecimal accruedInterest,
        BigDecimal prepaymentFee,
        BigDecimal otherCost,
        BigDecimal discountAmount,
        BigDecimal finalRepaymentAmount,
        LocalDateTime calculatedAt
) {
}
