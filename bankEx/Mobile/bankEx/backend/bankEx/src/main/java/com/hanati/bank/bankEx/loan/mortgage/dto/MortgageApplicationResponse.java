package com.hanati.bank.bankEx.loan.mortgage.dto;

import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos;

/**
 * 고객에게 보여 줄 신청 정보. 한도 산정 근거·심사 사유·담당자 정보는 내보내지 않는다.
 * approval·agreement는 해당 단계를 지나야 값이 있다.
 */
public record MortgageApplicationResponse(
        Long applicationId,
        String applicationNo,
        String productCode,
        Long requestedAmount,
        Integer termMonths,
        String repaymentMethod,
        String status,
        String statusLabel,
        String notice,
        String appliedAt,
        String propertyAddress,
        MortgageCoreDtos.Approval approval,
        MortgageCoreDtos.Agreement agreement,
        Long loanAccountId
) {
}
