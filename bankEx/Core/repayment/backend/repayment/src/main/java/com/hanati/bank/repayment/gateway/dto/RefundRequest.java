package com.hanati.bank.repayment.gateway.dto;

import java.math.BigDecimal;

public record RefundRequest(String accountNumber, BigDecimal amount, String description) {
}
