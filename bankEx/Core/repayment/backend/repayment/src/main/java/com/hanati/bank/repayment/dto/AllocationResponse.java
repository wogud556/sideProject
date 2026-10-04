package com.hanati.bank.repayment.dto;

import java.math.BigDecimal;

public record AllocationResponse(String type, BigDecimal amount, Long scheduleId) {
}
