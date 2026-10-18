package com.hanati.bank.mortgage.screening.service;

import jakarta.validation.constraints.NotNull;

public record ScreeningRequest(
        @NotNull Boolean incomeVerified,
        @NotNull Boolean ownershipVerified,
        String comment
) {
}
