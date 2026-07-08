package com.hanati.bank.bankEx.deposit.savings.domain;

import com.hanati.bank.bankEx.deposit.savings.enums.SavingsProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsProduct {
    private String productId;
    private String productName;
    private Double baseRate;
    private Double maxRate;
    private Long minAmount;
    private Long maxAmount;
    private Integer period;
    private String autoTransferYn;
    private SavingsProductStatus status;
    private LocalDateTime createdAt;
}
