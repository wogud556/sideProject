package com.hanati.bank.refinance.refinance.dto;

import com.hanati.bank.refinance.refinance.calculator.RepaymentAmountResult;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class RepaymentAmountResponse {
    private final Long loanId;
    private final BigDecimal principalBalance;
    private final BigDecimal accruedInterest;
    private final BigDecimal prepaymentFee;
    private final BigDecimal otherCost;
    private final BigDecimal discountAmount;
    private final BigDecimal finalRepaymentAmount;
    private final LocalDateTime calculatedAt;

    public RepaymentAmountResponse(RepaymentAmountResult result) {
        this.loanId = result.loanId();
        this.principalBalance = result.principalBalance();
        this.accruedInterest = result.accruedInterest();
        this.prepaymentFee = result.prepaymentFee();
        this.otherCost = result.otherCost();
        this.discountAmount = result.discountAmount();
        this.finalRepaymentAmount = result.finalRepaymentAmount();
        this.calculatedAt = result.calculatedAt();
    }
}
