package com.hanati.bank.bankEx.loan.jeonse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JeonseLoanProductResponse {
    private String productId;
    private String productName;
    private String productType;
    private Long maxLimitAmount;
    private Double baseRate;
    private Double minRate;
    private Double maxRate;
}
