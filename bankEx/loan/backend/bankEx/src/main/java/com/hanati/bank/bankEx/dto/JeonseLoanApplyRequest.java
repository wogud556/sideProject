package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JeonseLoanApplyRequest {
    private String userId;
    private String productId;
    private Long requestAmount;
    private Long annualIncome;
    private Long existingDebtAmount;
    private Integer creditScore;
    private String homelessYn;
    private String householderYn;
    private String guaranteeOrg;
    private String repaymentType;
    private boolean salaryTransferYn;
    private boolean cardUsageYn;
    private boolean autoTransferYn;
    private JeonseContractRequest contract;
}
