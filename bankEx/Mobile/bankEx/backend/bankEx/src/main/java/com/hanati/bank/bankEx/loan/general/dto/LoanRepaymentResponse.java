package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanRepaymentResponse {
    private Long applicationId;
    private String userId;
    private String accountNumber;
    private Long repaymentAmount;
    private Long remainingBalance;
    private String message;
}
