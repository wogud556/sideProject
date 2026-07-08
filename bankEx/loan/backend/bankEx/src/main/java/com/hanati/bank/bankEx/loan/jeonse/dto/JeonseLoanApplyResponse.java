package com.hanati.bank.bankEx.loan.jeonse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JeonseLoanApplyResponse {
    private String applicationId;
    private String status;
    private Long availableLimitAmount;
    private Long requestAmount;
    private Double estimatedRate;
    private String message;
}
