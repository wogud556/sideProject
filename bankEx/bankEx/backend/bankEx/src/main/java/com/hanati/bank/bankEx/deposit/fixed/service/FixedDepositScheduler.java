package com.hanati.bank.bankEx.deposit.fixed.service;

import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityBatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class FixedDepositScheduler {

    private final FixedDepositMaturityService fixedDepositMaturityService;

    public FixedDepositMaturityBatchResponse runMaturity() {
        return fixedDepositMaturityService.execute(LocalDate.now());
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void dailyBatch() {
        runMaturity();
    }
}
