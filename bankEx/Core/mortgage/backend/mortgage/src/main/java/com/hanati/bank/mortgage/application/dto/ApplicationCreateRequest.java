package com.hanati.bank.mortgage.application.dto;

import com.hanati.bank.mortgage.application.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** 고객 채널(bankEx)이 보내는 주담대 신청. 고객 이름은 bankEx가 회원 정보에서 채운다. */
public record ApplicationCreateRequest(
        @NotBlank String customerId,
        @NotBlank String customerName,
        @NotBlank String productCode,
        @NotNull @Positive Long requestedAmount,
        @NotNull @Positive Integer termMonths,
        @NotNull RepaymentMethod repaymentMethod,
        @NotNull LoanPurpose loanPurpose,
        @NotNull @PositiveOrZero Integer houseCount,
        @NotNull Boolean firstHomeBuyer,
        @NotNull @Valid Property property,
        @NotNull @Valid Income income,
        @NotNull @Valid List<Debt> debts
) {
    public record Property(
            @NotBlank String address,
            @NotNull RegionCode regionCode,
            @NotNull PropertyType propertyType,
            @Positive BigDecimal exclusiveArea,
            @NotNull @Positive Long declaredPrice
    ) {
    }

    public record Income(
            @NotNull IncomeType incomeType,
            @NotNull @Positive Long annualIncome,
            String employerName
    ) {
    }

    public record Debt(
            @NotNull DebtType debtType,
            String institution,
            @NotNull @PositiveOrZero Long balance,
            @NotNull @PositiveOrZero Long annualPrincipal,
            @NotNull @PositiveOrZero Long annualInterest
    ) {
    }
}
