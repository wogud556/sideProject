package com.hanati.bank.bankEx.loan.jeonse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JeonseContractRequest {
    private String lessorName;
    private String lessorPhone;
    private String houseAddress;
    private String houseType;
    private String capitalAreaYn;
    private Long depositAmount;
    private Long downPaymentAmount;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String fixedDateYn;
    private String moveInPlanYn;
    private Long seniorClaimAmount;
}
