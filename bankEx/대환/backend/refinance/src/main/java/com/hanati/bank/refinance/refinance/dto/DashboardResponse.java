package com.hanati.bank.refinance.refinance.dto;

public record DashboardResponse(
        long todayApplicationCount,
        long reviewingCount,
        long approvedCount,
        long executionPendingCount,
        long completedCount,
        long failedCount
) {
}
