package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FixedDepositProductResponse {
    private String depositProductId;
    private String productName;
    private Long minimumAmount;
    private Long maximumAmount;
    private Integer termMonths;
    private Double interestRate;
    private Double earlyTerminationRate;
    private String status;
}
