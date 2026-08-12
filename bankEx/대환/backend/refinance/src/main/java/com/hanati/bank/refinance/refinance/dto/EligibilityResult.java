package com.hanati.bank.refinance.refinance.dto;

public record EligibilityResult(Long loanId, String code, boolean passed, String message) {
}
