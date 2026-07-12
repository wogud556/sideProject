package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanDisbursementResponse {
    private Long applicationId;
    private String userId;
    private String accountNumber;
    private Long loanAmount;
    private String message;
}
