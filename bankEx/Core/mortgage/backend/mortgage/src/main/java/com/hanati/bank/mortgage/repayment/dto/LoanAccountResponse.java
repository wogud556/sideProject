package com.hanati.bank.mortgage.repayment.dto;

import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;

/** 대출 계좌와 다음 납부 회차. 모두 갚았으면 nextInstallment는 null이다. */
public record LoanAccountResponse(
        MortgageLoanAccount account,
        String statusLabel,
        String repaymentMethodLabel,
        MortgageRepaymentSchedule nextInstallment
) {
}
