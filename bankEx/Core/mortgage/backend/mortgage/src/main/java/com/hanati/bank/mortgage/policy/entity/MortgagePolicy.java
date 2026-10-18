package com.hanati.bank.mortgage.policy.entity;

import com.hanati.bank.mortgage.policy.enums.PolicyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 규제 정책. 비율을 코드에 넣지 않고 기준일·지역·차주·상품별 행으로 관리한다.
 * 조건 컬럼의 "ALL"은 모든 값에 적용된다는 뜻이다.
 * 행은 수정하지 않는다. 바꿀 때는 새 버전을 등록하고 이전 버전의 종료일을 닫는다.
 */
@Entity
@Table(name = "MORTGAGE_POLICY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgagePolicy {

    public static final String ALL = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POLICY_ID")
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "POLICY_TYPE", nullable = false, length = 20)
    private PolicyType policyType;

    @Column(name = "REGION_CODE", nullable = false, length = 20)
    private String regionCode;

    @Column(name = "BORROWER_TYPE", nullable = false, length = 20)
    private String borrowerType;

    @Column(name = "PRODUCT_CODE", nullable = false, length = 20)
    private String productCode;

    @Column(name = "POLICY_VALUE", nullable = false, precision = 15, scale = 2)
    private BigDecimal policyValue;

    @Column(name = "EFFECTIVE_FROM", nullable = false)
    private LocalDate effectiveFrom;

    /** null이면 종료일 없음 */
    @Column(name = "EFFECTIVE_TO")
    private LocalDate effectiveTo;

    @Column(name = "POLICY_VERSION", nullable = false)
    private Integer policyVersion;

    @Column(name = "BASIS", length = 200)
    private String basis;

    @Column(name = "CREATED_BY", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public boolean isEffectiveOn(LocalDate date) {
        return !effectiveFrom.isAfter(date) && (effectiveTo == null || !effectiveTo.isBefore(date));
    }

    public boolean sameKeyAs(MortgagePolicy other) {
        return policyType == other.policyType && regionCode.equals(other.regionCode)
                && borrowerType.equals(other.borrowerType) && productCode.equals(other.productCode);
    }

    public void closeAt(LocalDate lastDay) {
        this.effectiveTo = lastDay;
    }
}
