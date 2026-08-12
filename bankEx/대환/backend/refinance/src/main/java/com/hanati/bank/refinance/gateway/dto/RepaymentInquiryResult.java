package com.hanati.bank.refinance.gateway.dto;

import java.math.BigDecimal;

public record RepaymentInquiryResult(
        BigDecimal payoffAmount,
        String resultCode,
        String resultMessage
) {
}
