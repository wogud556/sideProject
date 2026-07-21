package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanRepaymentDetailResponse {
    private Long loanId;
    private Long paymentAmount;
    private Long principalAmount;
    private Long interestAmount;
    private Long remainingPrincipal;
    private String status;
}
