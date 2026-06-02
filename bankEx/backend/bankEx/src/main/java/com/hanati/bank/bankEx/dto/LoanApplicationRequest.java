package com.hanati.bank.bankEx.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoanApplicationRequest {
    private String userId;
    private String accountNumber;
    private Long productId;
    private Long requestAmount;
    private Integer loanPeriod;
}
