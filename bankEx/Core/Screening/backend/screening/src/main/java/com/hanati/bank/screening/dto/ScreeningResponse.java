package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.entity.LoanScreening;
import com.hanati.bank.screening.enums.LoanType;
import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ScreeningResponse {
    private Long screeningId;
    private String customerId;
    private String loanProductId;
    private LoanType loanType;
    private Long requestedAmount;
    private Long annualIncome;
    private Integer creditScore;
    private Long existingLoanAmount;
    private ScreeningStatus status;
    private Long approvedAmount;
    private BigDecimal approvedInterestRate;
    private ScreeningReasonCode reasonCode;
    private String reasonMessage;
    private LocalDateTime screenedAt;
    private String reviewerId;
    private String reviewComment;
    private LocalDateTime reviewedAt;

    public ScreeningResponse(LoanScreening s) {
        this.screeningId = s.getScreeningId();
        this.customerId = s.getCustomerId();
        this.loanProductId = s.getLoanProductId();
        this.loanType = s.getLoanType();
        this.requestedAmount = s.getRequestedAmount();
        this.annualIncome = s.getAnnualIncome();
        this.creditScore = s.getCreditScore();
        this.existingLoanAmount = s.getExistingLoanAmount();
        this.status = s.getStatus();
        this.approvedAmount = s.getApprovedAmount();
        this.approvedInterestRate = s.getApprovedInterestRate();
        this.reasonCode = s.getReasonCode();
        this.reasonMessage = s.getReasonCode().getMessage();
        this.screenedAt = s.getScreenedAt();
        this.reviewerId = s.getReviewerId();
        this.reviewComment = s.getReviewComment();
        this.reviewedAt = s.getReviewedAt();
    }
}
