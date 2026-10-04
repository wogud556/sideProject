package com.hanati.bank.bankEx.loan.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanProductResponse {
    private Long productId;
    private String productName;
    private Double interestRate;
    private Long maxLimit;
    private String description;
}
