package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanAccountValidateResponse {
    private boolean valid;
    private String userId;
    private String accountNumber;
    private String message;
}
