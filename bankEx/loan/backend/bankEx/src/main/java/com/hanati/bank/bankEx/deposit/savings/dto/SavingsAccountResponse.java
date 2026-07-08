package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SavingsAccountResponse {
    private String accountNo;
    private String productName;
    private Long monthlyAmount;
    private Integer period;
    private Integer currentCount;
    private Long balance;
    private Double interestRate;
    private String status;
    private LocalDate openDate;
    private LocalDate maturityDate;
}
