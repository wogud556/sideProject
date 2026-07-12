package com.hanati.bank.bankEx.deposit.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountCloseResponse {
    private Long accountId;
    private String accountNumber;
    private String accountStatus;
    private String closedAt;
    private String message;
}
