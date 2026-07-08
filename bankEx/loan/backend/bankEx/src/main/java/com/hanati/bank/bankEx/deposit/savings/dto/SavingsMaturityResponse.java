package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingsMaturityResponse {
    private String accountNo;
    private String status;
    private Long payoutAmount;
    private Long interestAmount;
    private Long taxAmount;
    private String message;
}
