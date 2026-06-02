package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanApplicationResponse {
    private Long applicationId;
    private String productName;
    private Long requestAmount;
    private Integer loanPeriod;
    private String status;
    private String createdAt;
}
