package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {
    private String userId;
    private String accountNumber;
    private Long productId;
    private Long requestAmount;
    private Integer loanPeriod;
}
