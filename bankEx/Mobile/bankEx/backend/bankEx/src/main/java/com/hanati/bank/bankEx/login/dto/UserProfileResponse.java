package com.hanati.bank.bankEx.login.dto;

import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String userName;
    private String phone;
    private String createdAt;
    private List<AccountResponse> accounts;
}
