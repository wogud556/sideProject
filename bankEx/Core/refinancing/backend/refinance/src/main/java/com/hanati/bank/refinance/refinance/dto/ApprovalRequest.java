package com.hanati.bank.refinance.refinance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalRequest {
    private BigDecimal approvedAmount;
    private String approvalCondition;
    private String approvalMemo;
}
