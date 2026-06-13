package com.hanati.bank.bankEx.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    private String userId;
    private String password;
}
