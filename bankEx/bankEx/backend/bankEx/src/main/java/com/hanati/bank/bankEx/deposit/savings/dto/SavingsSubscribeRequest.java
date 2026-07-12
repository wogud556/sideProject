package com.hanati.bank.bankEx.deposit.savings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SavingsSubscribeRequest {
    private String userId;
    private String withdrawAccountNo;
    private String productId;
    private Long monthlyAmount;
    private Integer period;
    private Integer transferDay;
    private boolean salaryTransferYn;
    private boolean mobileSignupYn;
    private boolean longTermYn;
}
