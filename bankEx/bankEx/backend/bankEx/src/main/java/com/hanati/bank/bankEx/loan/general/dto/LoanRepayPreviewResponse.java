package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanRepayPreviewResponse {
    private Long loanId;
    private Long remainingPrincipal;
    private Long expectedPrincipal;
    private Long expectedInterest;
    private Long expectedTotal;
    private String repaymentType;
}
