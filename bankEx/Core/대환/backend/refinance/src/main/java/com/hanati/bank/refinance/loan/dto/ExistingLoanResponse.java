package com.hanati.bank.refinance.loan.dto;

import com.hanati.bank.refinance.common.util.MaskUtil;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ExistingLoanResponse {

    private final Long loanId;
    private final String financialInstitutionName;
    private final String maskedLoanAccountNo;
    private final String loanProductName;
    private final String loanType;
    private final BigDecimal originalAmount;
    private final BigDecimal currentBalance;
    private final BigDecimal interestRate;
    private final LocalDate executionDate;
    private final LocalDate maturityDate;
    private final String repaymentMethod;
    private final boolean overdue;
    private final boolean refinanceEligible;

    public ExistingLoanResponse(ExistingLoan loan) {
        this.loanId = loan.getLoanId();
        this.financialInstitutionName = loan.getFinancialInstitutionName();
        this.maskedLoanAccountNo = MaskUtil.maskAccountNo(loan.getLoanAccountNo());
        this.loanProductName = loan.getLoanProductName();
        this.loanType = loan.getLoanType();
        this.originalAmount = loan.getOriginalAmount();
        this.currentBalance = loan.getCurrentBalance();
        this.interestRate = loan.getInterestRate();
        this.executionDate = loan.getExecutionDate();
        this.maturityDate = loan.getMaturityDate();
        this.repaymentMethod = loan.getRepaymentMethod();
        this.overdue = "Y".equals(loan.getOverdueYn());
        this.refinanceEligible = "ACTIVE".equals(loan.getStatus())
                && !this.overdue
                && loan.getCurrentBalance() != null
                && loan.getCurrentBalance().signum() > 0;
    }
}
