package com.hanati.bank.mortgage.limit.entity;

import com.hanati.bank.mortgage.application.enums.BorrowerType;
import com.hanati.bank.mortgage.application.enums.RegionCode;
import com.hanati.bank.mortgage.limit.enums.LimitConstraint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 한도 산정 결과. 입력값·산출값과 함께 적용한 정책 ID를 남겨, 정책이 바뀐 뒤에도 당시 결과를 재현할 수 있게 한다.
 * 재산정하면 새 행이 쌓이고, 심사는 가장 최근 행을 쓴다.
 */
@Entity
@Table(name = "MORTGAGE_LIMIT_RESULT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageLimitResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LIMIT_RESULT_ID")
    private Long limitResultId;

    @Column(name = "APPLICATION_ID", nullable = false)
    private Long applicationId;

    // ── 산정 기준 ──
    @Column(name = "BASE_DATE", nullable = false)
    private LocalDate baseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "REGION_CODE", nullable = false, length = 20)
    private RegionCode regionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "BORROWER_TYPE", nullable = false, length = 20)
    private BorrowerType borrowerType;

    @Column(name = "CREDIT_SCORE", nullable = false)
    private Integer creditScore;

    @Column(name = "APPLIED_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal appliedRate;

    // ── 입력값 ──
    @Column(name = "COLLATERAL_VALUE", nullable = false)
    private Long collateralValue;

    @Column(name = "SENIOR_CLAIMS", nullable = false)
    private Long seniorClaims;

    @Column(name = "ANNUAL_INCOME", nullable = false)
    private Long annualIncome;

    @Column(name = "EXISTING_DEBT_SERVICE", nullable = false)
    private Long existingDebtService;

    @Column(name = "REQUESTED_AMOUNT", nullable = false)
    private Long requestedAmount;

    // ── 적용 정책 (값 + 정책 ID) ──
    @Column(name = "LTV_PERCENT", nullable = false, precision = 5, scale = 2)
    private BigDecimal ltvPercent;

    @Column(name = "DTI_PERCENT", nullable = false, precision = 5, scale = 2)
    private BigDecimal dtiPercent;

    @Column(name = "DSR_PERCENT", nullable = false, precision = 5, scale = 2)
    private BigDecimal dsrPercent;

    @Column(name = "STRESS_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal stressRate;

    @Column(name = "LTV_POLICY_ID", nullable = false)
    private Long ltvPolicyId;

    @Column(name = "DTI_POLICY_ID", nullable = false)
    private Long dtiPolicyId;

    @Column(name = "DSR_POLICY_ID", nullable = false)
    private Long dsrPolicyId;

    @Column(name = "STRESS_POLICY_ID", nullable = false)
    private Long stressPolicyId;

    @Column(name = "MAX_AMOUNT_POLICY_ID", nullable = false)
    private Long maxAmountPolicyId;

    // ── 산출값 ──
    @Column(name = "LTV_LIMIT", nullable = false)
    private Long ltvLimit;

    @Column(name = "COLLATERAL_LIMIT", nullable = false)
    private Long collateralLimit;

    @Column(name = "DTI_LIMIT", nullable = false)
    private Long dtiLimit;

    @Column(name = "DSR_LIMIT", nullable = false)
    private Long dsrLimit;

    @Column(name = "STRESS_DSR_LIMIT", nullable = false)
    private Long stressDsrLimit;

    @Column(name = "PRODUCT_LIMIT", nullable = false)
    private Long productLimit;

    @Column(name = "REGULATORY_LIMIT", nullable = false)
    private Long regulatoryLimit;

    @Column(name = "FINAL_LIMIT", nullable = false)
    private Long finalLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "BINDING_CONSTRAINT", nullable = false, length = 20)
    private LimitConstraint bindingConstraint;

    @Column(name = "REQUESTED_LTV", nullable = false, precision = 7, scale = 2)
    private BigDecimal requestedLtv;

    @Column(name = "REQUESTED_DTI", nullable = false, precision = 7, scale = 2)
    private BigDecimal requestedDti;

    @Column(name = "REQUESTED_DSR", nullable = false, precision = 7, scale = 2)
    private BigDecimal requestedDsr;

    @Column(name = "REQUESTED_STRESS_DSR", nullable = false, precision = 7, scale = 2)
    private BigDecimal requestedStressDsr;

    @Column(name = "CALCULATED_BY", nullable = false, length = 20)
    private String calculatedBy;

    @Column(name = "CALCULATED_AT", nullable = false)
    private LocalDateTime calculatedAt;
}
