package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SavingsSubscribeResponse {
    private String accountNo;
    private String status;
    private Double interestRate;
    private Long monthlyAmount;
    private Integer period;
    private LocalDate openDate;
    private LocalDate maturityDate;
    private String message;
}
