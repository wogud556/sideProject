package com.hanati.bank.bankEx.loan.jeonse.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseLoanProduct {
    private String productId;
    private String productName;
    private String productType;
    private Long maxLimitAmount;
    private Double baseRate;
    private Double minRate;
    private Double maxRate;
    private String useYn;
    private LocalDateTime createdAt;
}
