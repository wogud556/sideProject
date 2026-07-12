package com.hanati.bank.bankEx.login.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String userId;
    private String password;
    private String userName;
    private String phone;
    private String accountPassword;
}
