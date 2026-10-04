package com.hanati.bank.bankEx.deposit.general.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    private Long amount;
    private String description;
}
