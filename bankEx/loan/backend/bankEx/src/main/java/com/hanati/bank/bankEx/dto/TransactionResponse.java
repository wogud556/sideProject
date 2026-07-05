package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionResponse {
    private String accountNumber;
    private String transactionType;
    private Long amount;
    private Long balanceAfter;
    private String description;
    private String transactionAt;
}
