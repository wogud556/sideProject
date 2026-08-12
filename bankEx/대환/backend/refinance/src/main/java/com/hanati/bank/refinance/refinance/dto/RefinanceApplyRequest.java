package com.hanati.bank.refinance.refinance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RefinanceApplyRequest {
    private Long customerId;
    private List<Long> loanIds;

    private String newLoanProductName;
    private BigDecimal newLoanAmount;
    private BigDecimal newLoanRate;
    private String newLoanRateType;
    private Integer newLoanPeriodMonths;
    private String newLoanRepaymentMethod;
    private LocalDate newLoanExecutionScheduledDate;
    private String newLoanAccountNo;
    private String refinancePurposeYn;
}
