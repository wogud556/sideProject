package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "JEONSE_CONTRACT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseContract {

    @Id
    @Column(name = "CONTRACT_ID", length = 40)
    private String contractId;

    @Column(name = "APPLICATION_ID", nullable = false, length = 40)
    private String applicationId;

    @Column(name = "LESSOR_NAME", nullable = false, length = 100)
    private String lessorName;

    @Column(name = "LESSOR_PHONE", length = 30)
    private String lessorPhone;

    @Column(name = "HOUSE_ADDRESS", nullable = false, length = 300)
    private String houseAddress;

    @Column(name = "HOUSE_TYPE", nullable = false, length = 30)
    private String houseType;

    @Column(name = "CAPITAL_AREA_YN", nullable = false, length = 1)
    private String capitalAreaYn;

    @Column(name = "DEPOSIT_AMOUNT", nullable = false)
    private Long depositAmount;

    @Column(name = "DOWN_PAYMENT_AMOUNT", nullable = false)
    private Long downPaymentAmount;

    @Column(name = "CONTRACT_START_DATE", nullable = false)
    private LocalDate contractStartDate;

    @Column(name = "CONTRACT_END_DATE", nullable = false)
    private LocalDate contractEndDate;

    @Column(name = "FIXED_DATE_YN", length = 1)
    private String fixedDateYn;

    @Column(name = "MOVE_IN_PLAN_YN", length = 1)
    private String moveInPlanYn;

    @Column(name = "SENIOR_CLAIM_AMOUNT")
    private Long seniorClaimAmount;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
