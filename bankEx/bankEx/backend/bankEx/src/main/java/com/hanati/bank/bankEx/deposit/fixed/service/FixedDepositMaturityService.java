package com.hanati.bank.bankEx.deposit.fixed.service;

import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositAccount;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositStatus;
import com.hanati.bank.bankEx.deposit.fixed.mapper.FixedDepositAccountMapper;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FixedDepositMaturityService {

    private final FixedDepositAccountMapper fixedDepositAccountMapper;
    private final transService transService;
    private final FixedDepositInterestCalculator fixedDepositInterestCalculator;

    @Transactional
    public FixedDepositMaturityBatchResponse execute(LocalDate today) {
        List<FixedDepositAccount> targets = fixedDepositAccountMapper.findDueForMaturity(today);
        for (FixedDepositAccount account : targets) {
            processMaturity(account);
        }
        return new FixedDepositMaturityBatchResponse(targets.size());
    }

    /**
     * 만기 이자는 가입 시점에 확정되므로 계좌에 저장된 EXPECTED_INTEREST를 그대로 지급한다.
     * (원금 + 세전이자 - 이자소득세 = EXPECTED_MATURITY_AMOUNT)
     */
    private void processMaturity(FixedDepositAccount account) {
        long interest = account.getExpectedInterest();
        long tax = fixedDepositInterestCalculator.calculateTax(interest);
        long payout = account.getPrincipalAmount() + interest - tax;

        transService.credit(account.getWithdrawAccountNo(), payout,
                "FIXED_DEPOSIT_MATURITY", "정기예금 만기 원리금 지급 - " + account.getDepositAccountId());

        account.setStatus(FixedDepositStatus.MATURED);
        account.setUpdatedAt(LocalDateTime.now());
        fixedDepositAccountMapper.update(account);
    }
}
