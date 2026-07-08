package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JeonseLoanReviewResponse {
    private String applicationId;
    private String status;
    private Long approvedAmount;
    private Double loanRate;
    private String message;
}
