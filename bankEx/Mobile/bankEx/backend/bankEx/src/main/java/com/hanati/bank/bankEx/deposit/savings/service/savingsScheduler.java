package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.deposit.savings.dto.AutoTransferBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.MaturityBatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class savingsScheduler {

    private final autoTransferService autoTransferService;
    private final savingsMaturityService savingsMaturityService;

    public AutoTransferBatchResponse runAutoTransfer() {
        return autoTransferService.execute(LocalDate.now());
    }

    public MaturityBatchResponse runMaturity() {
        return savingsMaturityService.execute(LocalDate.now());
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void dailyBatch() {
        runAutoTransfer();
        runMaturity();
    }
}
