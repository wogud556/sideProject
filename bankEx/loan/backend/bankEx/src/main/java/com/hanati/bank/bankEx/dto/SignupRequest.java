package com.hanati.bank.bankEx.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    private String userId;
    private String password;
    private String userName;
    private String phone;
}
