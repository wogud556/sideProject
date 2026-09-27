package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FixedDepositTerminateResponse {
    private String depositAccountId;
    private String status;
    private Long payoutAmount;
    private Long interestAmount;
    private Long taxAmount;
    private String message;
}
