package com.hanati.bank.bankEx.deposit.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String requestId;
    private String userId;
    private String withdrawalAccountNumber;
    private String depositAccountNumber;
    private Long amount;
    private String accountPassword;
    private String withdrawalMemo;
    private String depositMemo;
}
