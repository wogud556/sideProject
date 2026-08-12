package com.hanati.bank.refinance.refinance.dto;

import com.hanati.bank.refinance.common.util.MaskUtil;
import com.hanati.bank.refinance.refinance.entity.RefinanceTarget;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RefinanceTargetResponse {
    private final Long targetId;
    private final Long loanId;
    private final String financialInstitutionCode;
    private final String maskedLoanAccountNo;
    private final String loanProductCode;
    private final BigDecimal loanBalance;
    private final BigDecimal repaymentAmount;
    private final BigDecimal prepaymentFee;
    private final BigDecimal interestAmount;

    public RefinanceTargetResponse(RefinanceTarget target) {
        this.targetId = target.getTargetId();
        this.loanId = target.getLoanId();
        this.financialInstitutionCode = target.getFinancialInstitutionCode();
        this.maskedLoanAccountNo = MaskUtil.maskAccountNo(target.getLoanAccountNo());
        this.loanProductCode = target.getLoanProductCode();
        this.loanBalance = target.getLoanBalance();
        this.repaymentAmount = target.getRepaymentAmount();
        this.prepaymentFee = target.getPrepaymentFee();
        this.interestAmount = target.getInterestAmount();
    }
}
