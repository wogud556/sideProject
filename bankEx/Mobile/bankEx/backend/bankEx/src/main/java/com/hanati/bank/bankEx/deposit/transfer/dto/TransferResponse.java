package com.hanati.bank.bankEx.deposit.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferResponse {
    private Long transferId;
    private String transactionNumber;
    private String status;
    private Long amount;
    private Long balanceAfterTransfer;
    private String transferredAt;
}
