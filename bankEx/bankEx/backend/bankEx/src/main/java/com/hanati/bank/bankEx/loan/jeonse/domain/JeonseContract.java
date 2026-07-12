package com.hanati.bank.bankEx.loan.jeonse.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseContract {
    private String contractId;
    private String applicationId;
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
    private LocalDateTime createdAt;
}
