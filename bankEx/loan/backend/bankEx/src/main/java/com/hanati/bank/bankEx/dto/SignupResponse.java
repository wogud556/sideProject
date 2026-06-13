package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {
    private String userId;
    private String userName;
    private String accountNumber;
    private String message;
}
