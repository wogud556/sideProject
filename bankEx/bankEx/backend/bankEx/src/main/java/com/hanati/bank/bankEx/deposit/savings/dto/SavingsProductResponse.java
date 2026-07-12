package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingsProductResponse {
    private String productId;
    private String productName;
    private Double baseRate;
    private Double maxRate;
    private Long minAmount;
    private Long maxAmount;
    private Integer period;
    private String autoTransferYn;
    private String status;
}
