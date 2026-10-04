package com.hanati.bank.repayment.gateway.dto;

import java.math.BigDecimal;

public record WithdrawalResult(boolean success, String withdrawalTransactionNo,
                                BigDecimal amount, String resultCode, String message) {
}
