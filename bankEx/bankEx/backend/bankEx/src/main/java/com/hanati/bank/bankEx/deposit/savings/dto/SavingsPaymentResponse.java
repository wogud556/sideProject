package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingsPaymentResponse {
    private String accountNo;
    private Integer paymentSeq;
    private Long paymentAmount;
    private Long balance;
    private String status;
    private String message;
}
