package com.hanati.bank.repayment.gateway.dto;

import java.math.BigDecimal;

public record WithdrawalRequest(String accountNumber, BigDecimal amount, String description) {
}
