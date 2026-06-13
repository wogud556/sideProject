package com.hanati.bank.screening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanApplicationRequest {
    @NotBlank
    private String userId;
    @NotNull
    private Long productId;
    @NotNull
    @Positive
    private Long requestAmount;
}
