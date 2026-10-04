package com.hanati.bank.bankEx.deposit.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountOpenRequest {
    private String userId;
    private String productCode;
    private String accountName;
    private String accountPassword;
}
