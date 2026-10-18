package com.hanati.bank.mortgage.gateway;

public class DisbursementFailedException extends RuntimeException {
    public DisbursementFailedException(String message) {
        super(message);
    }
}
