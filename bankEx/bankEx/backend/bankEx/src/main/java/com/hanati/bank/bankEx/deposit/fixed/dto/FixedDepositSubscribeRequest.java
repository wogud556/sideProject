package com.hanati.bank.bankEx.deposit.fixed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FixedDepositSubscribeRequest {
    private String userId;
    private String depositProductId;
    private String withdrawAccountNo;
    private Long principalAmount;
    private Integer termMonths;
}
