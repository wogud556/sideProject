package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningRequest {
    @NotBlank
    private String customerId;
    @NotBlank
    private String loanProductId;
    @NotNull
    private LoanType loanType;
    @NotNull
    @Positive
    private Long requestedAmount;
    @NotNull
    @PositiveOrZero
    private Long annualIncome;
    @NotNull
    @Min(0)
    @Max(1000)
    private Integer creditScore;
    @NotNull
    @PositiveOrZero
    private Long existingLoanAmount;
}
