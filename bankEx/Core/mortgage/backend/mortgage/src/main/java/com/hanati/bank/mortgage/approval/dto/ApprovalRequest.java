package com.hanati.bank.mortgage.approval.dto;

import jakarta.validation.constraints.Positive;

/** approvedAmount를 비우면 심사 권고 금액으로 승인한다. */
public record ApprovalRequest(
        @Positive Long approvedAmount,
        String comment
) {
}
