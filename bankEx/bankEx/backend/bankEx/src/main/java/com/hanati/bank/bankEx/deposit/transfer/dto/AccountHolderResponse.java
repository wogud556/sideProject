package com.hanati.bank.bankEx.deposit.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountHolderResponse {
    private String accountNumber;
    private String accountHolderName;
    private String accountStatus;
}
