package com.hanati.bank.bankEx.deposit.fixed.domain;

import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDepositProduct {
    private String depositProductId;
    private String productName;
    private Long minimumAmount;
    private Long maximumAmount;
    private Integer termMonths;
    private Double interestRate;
    private Double earlyTerminationRate;
    private FixedDepositProductStatus status;
    private LocalDateTime createdAt;
}
