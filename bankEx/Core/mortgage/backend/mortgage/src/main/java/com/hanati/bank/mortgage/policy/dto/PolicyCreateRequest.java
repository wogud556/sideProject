package com.hanati.bank.mortgage.policy.dto;

import com.hanati.bank.mortgage.policy.enums.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyCreateRequest(
        @NotNull PolicyType policyType,
        @NotBlank String regionCode,
        @NotBlank String borrowerType,
        @NotBlank String productCode,
        @NotNull @PositiveOrZero BigDecimal policyValue,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String basis
) {
}
