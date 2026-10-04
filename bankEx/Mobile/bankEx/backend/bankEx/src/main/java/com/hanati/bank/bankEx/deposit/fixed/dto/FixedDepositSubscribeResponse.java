package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class FixedDepositSubscribeResponse {
    private String depositAccountId;
    private String status;
    private Long principalAmount;
    private Double interestRate;
    private Integer termMonths;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private Long expectedInterest;
    private Long expectedMaturityAmount;
    private String message;
}
