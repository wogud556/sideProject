package com.hanati.bank.bankEx.deposit.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferResultResponse {
    private Long transferId;
    private String transactionNumber;
    private String withdrawalAccountNumber;
    private String depositAccountNumber;
    private String depositAccountHolderName;
    private Long amount;
    private Long balanceAfterTransfer;
    private String transferStatus;
    private String transferredAt;
}
