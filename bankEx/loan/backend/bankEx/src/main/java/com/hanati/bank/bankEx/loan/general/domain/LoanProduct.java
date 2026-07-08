package com.hanati.bank.bankEx.loan.general.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {
    private Long productId;
    private String productName;
    private Double interestRate;
    private Long maxLimit;
    private String description;
}
