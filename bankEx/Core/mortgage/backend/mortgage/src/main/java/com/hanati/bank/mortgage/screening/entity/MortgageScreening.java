package com.hanati.bank.mortgage.screening.entity;

import com.hanati.bank.mortgage.screening.enums.AreaResult;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 여신 심사 이력. 추가 서류 후 재심사하면 새 행이 쌓인다. */
@Entity
@Table(name = "MORTGAGE_SCREENING")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCREENING_ID")
    private Long screeningId;

    @Column(name = "APPLICATION_ID", nullable = false)
    private Long applicationId;

    /** 심사 근거가 된 한도 산정 결과 */
    @Column(name = "LIMIT_RESULT_ID", nullable = false)
    private Long limitResultId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private ScreeningStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "BORROWER_RESULT", nullable = false, length = 10)
    private AreaResult borrowerResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "COLLATERAL_RESULT", nullable = false, length = 10)
    private AreaResult collateralResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "REGULATORY_RESULT", nullable = false, length = 10)
    private AreaResult regulatoryResult;

    /** ScreeningReason 이름을 쉼표로 이어 붙인 값 */
    @Column(name = "REASON_CODES", length = 300)
    private String reasonCodes;

    @Column(name = "RECOMMENDED_AMOUNT")
    private Long recommendedAmount;

    @Column(name = "REQUIRES_PRE_EXECUTION_CHECK", nullable = false)
    private Boolean requiresPreExecutionCheck;

    @Column(name = "INCOME_VERIFIED", nullable = false)
    private Boolean incomeVerified;

    @Column(name = "OWNERSHIP_VERIFIED", nullable = false)
    private Boolean ownershipVerified;

    @Column(name = "SCREENER_ID", nullable = false, length = 20)
    private String screenerId;

    @Column(name = "SCREENING_COMMENT", length = 500)
    private String screeningComment;

    @Column(name = "SCREENED_AT", nullable = false)
    private LocalDateTime screenedAt;
}
