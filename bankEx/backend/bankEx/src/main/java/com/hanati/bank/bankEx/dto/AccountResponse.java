package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountResponse {
    private Long accountId;
    private String accountNumber;
    private Long balance;
    private String createdAt;
}
