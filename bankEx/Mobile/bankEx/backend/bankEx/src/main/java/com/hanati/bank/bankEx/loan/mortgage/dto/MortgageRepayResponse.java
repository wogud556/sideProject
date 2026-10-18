package com.hanati.bank.bankEx.loan.mortgage.dto;

public record MortgageRepayResponse(
        Integer installmentNo,
        Long totalPaid,
        Long principalPaid,
        Long interestPaid,
        Long outstandingAfter,
        String sourceAccountNumber,
        Long balanceAfter,
        String transactionNumber
) {
}
