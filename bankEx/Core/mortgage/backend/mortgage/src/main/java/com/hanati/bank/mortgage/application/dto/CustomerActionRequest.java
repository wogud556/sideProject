package com.hanati.bank.mortgage.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerActionRequest(@NotBlank String customerId) {
}
