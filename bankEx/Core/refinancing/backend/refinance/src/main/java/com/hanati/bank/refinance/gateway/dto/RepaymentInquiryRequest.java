package com.hanati.bank.refinance.gateway.dto;

public record RepaymentInquiryRequest(
        String financialInstitutionCode,
        String loanAccountNo
) {
}
