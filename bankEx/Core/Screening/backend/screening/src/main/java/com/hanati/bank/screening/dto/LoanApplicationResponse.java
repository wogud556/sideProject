package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.entity.LoanApplication;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class LoanApplicationResponse {
    private Long applicationId;
    private String userId;
    private Long productId;
    private Long requestAmount;
    private String applicationStatus;
    private LocalDateTime createdAt;

    public LoanApplicationResponse(LoanApplication a) {
        this.applicationId = a.getApplicationId();
        this.userId = a.getUserId();
        this.productId = a.getProductId();
        this.requestAmount = a.getRequestAmount();
        this.applicationStatus = a.getApplicationStatus();
        this.createdAt = a.getCreatedAt();
    }
}
