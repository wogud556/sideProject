package com.hanati.bank.bankEx.loan.mortgage.dto;

import java.math.BigDecimal;
import java.util.List;

/** 고객 화면의 주담대 신청. 값 검증은 Core가 하고, bankEx는 회원 여부만 확인한다. */
public record MortgageApplyRequest(
        String userId,
        String productCode,
        Long requestedAmount,
        Integer termMonths,
        String repaymentMethod,
        String loanPurpose,
        Integer houseCount,
        Boolean firstHomeBuyer,
        Property property,
        Income income,
        List<Debt> debts
) {
    public record Property(String address, String regionCode, String propertyType, BigDecimal exclusiveArea,
                           Long declaredPrice) {
    }

    public record Income(String incomeType, Long annualIncome, String employerName) {
    }

    public record Debt(String debtType, String institution, Long balance, Long annualPrincipal, Long annualInterest) {
    }
}
