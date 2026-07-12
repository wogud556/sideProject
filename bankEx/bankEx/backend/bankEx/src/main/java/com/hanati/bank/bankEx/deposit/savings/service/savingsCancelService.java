package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsAccount;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsCancelResponse;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsAccountStatus;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsAccountMapper;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class savingsCancelService {

    private final SavingsAccountMapper savingsAccountMapper;
    private final SavingsPaymentMapper savingsPaymentMapper;
    private final transService transService;
    private final savingsInterestService savingsInterestService;

    @Transactional
    public SavingsCancelResponse cancel(String accountNo) {
        SavingsAccount account = savingsAccountMapper.findByAccountNo(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_ACCOUNT_NOT_FOUND));
        if (account.getStatus() != SavingsAccountStatus.OPEN && account.getStatus() != SavingsAccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.SAVINGS_ALREADY_TERMINATED);
        }

        List<SavingsPaymentHistory> payments = savingsPaymentMapper.findByAccountNo(accountNo);
        double rateRatio = savingsInterestService.earlyTerminationRateRatio(account.getCurrentCount(), account.getPeriod());
        double earlyRate = account.getInterestRate() * rateRatio;
        long interest = savingsInterestService.calculateInterest(payments, earlyRate, account.getCurrentCount());
        long tax = savingsInterestService.calculateTax(interest);
        long payout = account.getBalance() + interest - tax;

        if (payout > 0) {
            transService.credit(account.getWithdrawAccountNo(), payout,
                    "SAVINGS_CANCEL", "적금 중도해지 원리금 지급 - " + accountNo);
        }

        account.setStatus(SavingsAccountStatus.CANCELLED);
        account.setUpdatedAt(LocalDateTime.now());
        savingsAccountMapper.update(account);

        return new SavingsCancelResponse(accountNo, account.getStatus().name(), payout, interest, tax, "중도해지가 완료되었습니다.");
    }
}
