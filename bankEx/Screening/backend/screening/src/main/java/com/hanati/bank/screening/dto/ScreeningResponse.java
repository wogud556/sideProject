package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.entity.LoanScreeningResult;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class ScreeningResponse {
    private Long applicationId;
    private Integer cssScore;
    private BigDecimal dsrRate;
    private String resultStatus;
    private Long approvedAmount;
    private BigDecimal interestRate;
    private String rejectReason;

    public ScreeningResponse(LoanScreeningResult r) {
        this.applicationId = r.getApplicationId();
        this.cssScore = r.getCssScore();
        this.dsrRate = r.getDsrRate();
        this.resultStatus = r.getResultStatus();
        this.approvedAmount = r.getApprovedAmount();
        this.interestRate = r.getInterestRate();
        this.rejectReason = r.getRejectReason();
    }
}
