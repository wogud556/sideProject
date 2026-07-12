package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsAccount;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.dto.AutoTransferBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.enums.PaymentStatus;
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
public class autoTransferService {

    private final SavingsAccountMapper savingsAccountMapper;
    private final SavingsPaymentMapper savingsPaymentMapper;
    private final transService transService;

    @Transactional
    public AutoTransferBatchResponse execute(LocalDate today) {
        List<SavingsAccount> targets = savingsAccountMapper.findDueForTransfer(today.getDayOfMonth());

        int success = 0;
        int failed = 0;
        for (SavingsAccount account : targets) {
            int nextSeq = account.getCurrentCount() + 1;
            if (savingsPaymentMapper.countSuccessByAccountNoAndSeq(account.getAccountNo(), nextSeq) > 0) {
                continue;
            }
            if (processSinglePayment(account, nextSeq, today)) {
                success++;
            } else {
                failed++;
            }
        }

        return new AutoTransferBatchResponse(targets.size(), success, failed);
    }

    private boolean processSinglePayment(SavingsAccount account, int nextSeq, LocalDate today) {
        LocalDateTime now = LocalDateTime.now();

        try {
            transService.debit(account.getWithdrawAccountNo(), account.getMonthlyAmount(),
                    "SAVINGS_AUTO_TRANSFER", "적금 자동이체 - " + account.getAccountNo());
        } catch (BusinessException e) {
            savingsPaymentMapper.insert(SavingsPaymentHistory.builder()
                    .accountNo(account.getAccountNo())
                    .paymentSeq(nextSeq)
                    .paymentDate(today)
                    .paymentAmount(account.getMonthlyAmount())
                    .status(PaymentStatus.FAILED)
                    .createdAt(now)
                    .build());
            return false;
        }

        savingsPaymentMapper.insert(SavingsPaymentHistory.builder()
                .accountNo(account.getAccountNo())
                .paymentSeq(nextSeq)
                .paymentDate(today)
                .paymentAmount(account.getMonthlyAmount())
                .status(PaymentStatus.SUCCESS)
                .createdAt(now)
                .build());

        account.setCurrentCount(nextSeq);
        account.setBalance(account.getBalance() + account.getMonthlyAmount());
        account.setUpdatedAt(now);
        savingsAccountMapper.update(account);
        return true;
    }
}
