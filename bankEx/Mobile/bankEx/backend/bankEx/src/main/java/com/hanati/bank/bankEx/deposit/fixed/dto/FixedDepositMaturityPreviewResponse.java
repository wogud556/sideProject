package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class FixedDepositMaturityPreviewResponse {
    private String depositAccountId;
    private Long principalAmount;
    private Double interestRate;
    private LocalDate maturityDate;
    private Long expectedInterest;
    private Long taxAmount;
    private Long expectedMaturityAmount;
}
