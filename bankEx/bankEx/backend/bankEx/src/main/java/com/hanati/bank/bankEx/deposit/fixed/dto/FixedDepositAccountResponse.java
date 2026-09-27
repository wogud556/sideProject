package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class FixedDepositAccountResponse {
    private String depositAccountId;
    private String productName;
    private Long principalAmount;
    private Double interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private Long expectedInterest;
    private Long expectedMaturityAmount;
    private String status;
}
