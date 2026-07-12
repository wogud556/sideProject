package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AutoTransferBatchResponse {
    private int processedCount;
    private int successCount;
    private int failedCount;
}
