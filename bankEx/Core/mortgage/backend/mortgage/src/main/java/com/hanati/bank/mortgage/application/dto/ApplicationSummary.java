package com.hanati.bank.mortgage.application.dto;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationSummary(
        Long applicationId,
        String applicationNo,
        String customerId,
        String customerName,
        String productCode,
        Long requestedAmount,
        ApplicationStatus status,
        String statusLabel,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt
) {
    public static ApplicationSummary from(MortgageApplication a) {
        return new ApplicationSummary(a.getApplicationId(), a.getApplicationNo(), a.getCustomerId(),
                a.getCustomerName(), a.getProductCode(), a.getRequestedAmount(), a.getStatus(),
                a.getStatus().getLabel(), a.getAppliedAt(), a.getUpdatedAt());
    }
}
