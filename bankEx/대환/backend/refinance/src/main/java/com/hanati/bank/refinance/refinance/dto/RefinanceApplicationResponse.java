package com.hanati.bank.refinance.refinance.dto;

import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RefinanceApplicationResponse {
    private final Long applicationId;
    private final String applicationNo;
    private final Long customerId;
    private final RefinanceStatus status;
    private final LocalDateTime applicationDate;
    private final BigDecimal requestedAmount;
    private final BigDecimal approvedAmount;

    private final String newLoanProductName;
    private final BigDecimal newLoanAmount;
    private final BigDecimal newLoanRate;
    private final String newLoanRateType;
    private final Integer newLoanPeriodMonths;
    private final LocalDate newLoanMaturityDate;
    private final String newLoanRepaymentMethod;
    private final LocalDate newLoanExecutionScheduledDate;
    private final String newLoanAccountNo;
    private final String refinancePurposeYn;
    private final String rejectReasonCode;

    private final List<RefinanceTargetResponse> targets;

    public RefinanceApplicationResponse(RefinanceApplication app, List<RefinanceTargetResponse> targets) {
        this.applicationId = app.getApplicationId();
        this.applicationNo = app.getApplicationNo();
        this.customerId = app.getCustomerId();
        this.status = app.getStatus();
        this.applicationDate = app.getApplicationDate();
        this.requestedAmount = app.getRequestedAmount();
        this.approvedAmount = app.getApprovedAmount();
        this.newLoanProductName = app.getNewLoanProductName();
        this.newLoanAmount = app.getNewLoanAmount();
        this.newLoanRate = app.getNewLoanRate();
        this.newLoanRateType = app.getNewLoanRateType();
        this.newLoanPeriodMonths = app.getNewLoanPeriodMonths();
        this.newLoanMaturityDate = app.getNewLoanMaturityDate();
        this.newLoanRepaymentMethod = app.getNewLoanRepaymentMethod();
        this.newLoanExecutionScheduledDate = app.getNewLoanExecutionScheduledDate();
        this.newLoanAccountNo = app.getNewLoanAccountNo();
        this.refinancePurposeYn = app.getRefinancePurposeYn();
        this.rejectReasonCode = app.getRejectReasonCode();
        this.targets = targets;
    }
}
