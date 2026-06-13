package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String userId;
    private String userName;
    private String message;
}
