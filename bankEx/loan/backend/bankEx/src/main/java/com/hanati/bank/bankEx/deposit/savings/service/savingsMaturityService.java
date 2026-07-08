package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsAccount;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.dto.MaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsAccountStatus;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsAccountMapper;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class savingsMaturityService {

    private final SavingsAccountMapper savingsAccountMapper;
    private final SavingsPaymentMapper savingsPaymentMapper;
    private final transService transService;
    private final savingsInterestService savingsInterestService;

    @Transactional
    public MaturityBatchResponse execute(LocalDate today) {
        List<SavingsAccount> targets = savingsAccountMapper.findDueForMaturity(today);
        for (SavingsAccount account : targets) {
            processMaturity(account);
        }
        return new MaturityBatchResponse(targets.size());
    }

    private void processMaturity(SavingsAccount account) {
        List<SavingsPaymentHistory> payments = savingsPaymentMapper.findByAccountNo(account.getAccountNo());
        long interest = savingsInterestService.calculateInterest(payments, account.getInterestRate(), account.getPeriod());
        long tax = savingsInterestService.calculateTax(interest);
        long payout = account.getBalance() + interest - tax;

        if (payout > 0) {
            transService.credit(account.getWithdrawAccountNo(), payout,
                    "SAVINGS_MATURITY", "적금 만기 원리금 지급 - " + account.getAccountNo());
        }

        account.setStatus(SavingsAccountStatus.TERMINATED);
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountMapper.update(account);
    }
}
