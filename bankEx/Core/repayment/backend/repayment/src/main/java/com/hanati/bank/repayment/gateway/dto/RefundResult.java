package com.hanati.bank.repayment.gateway.dto;

public record RefundResult(boolean success, String refundTransactionNo, String resultCode, String message) {
}
