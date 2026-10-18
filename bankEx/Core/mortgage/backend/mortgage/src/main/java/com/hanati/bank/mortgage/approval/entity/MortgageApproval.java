package com.hanati.bank.mortgage.approval.entity;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 대출 승인. 승인 금액·금리·기간을 확정한다. 상품 조건이 나중에 바뀌어도 이 값은 바뀌지 않는다. */
@Entity
@Table(name = "MORTGAGE_APPROVAL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPROVAL_ID")
    private Long approvalId;

    @Column(name = "APPLICATION_ID", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "SCREENING_ID", nullable = false)
    private Long screeningId;

    @Column(name = "APPROVED_AMOUNT", nullable = false)
    private Long approvedAmount;

    @Column(name = "INTEREST_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "TERM_MONTHS", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPAYMENT_METHOD", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    /** 조건부 승인의 조건. 없으면 null */
    @Column(name = "CONDITIONS", length = 500)
    private String conditions;

    /** 실행 전에 확인해야 할 조건이 남아 있으면 false */
    @Column(name = "CONDITIONS_FULFILLED", nullable = false)
    private Boolean conditionsFulfilled;

    @Column(name = "CONDITIONS_CONFIRMED_BY", length = 20)
    private String conditionsConfirmedBy;

    @Column(name = "APPROVER_ID", nullable = false, length = 20)
    private String approverId;

    @Column(name = "APPROVAL_COMMENT", length = 500)
    private String approvalComment;

    @Column(name = "APPROVED_AT", nullable = false)
    private LocalDateTime approvedAt;

    /** 승인 유효기간 마지막 날. 이 날까지 약정과 실행을 마쳐야 한다 */
    @Column(name = "EXPIRES_ON", nullable = false)
    private LocalDate expiresOn;

    public boolean isExpiredOn(LocalDate date) {
        return date.isAfter(expiresOn);
    }

    public void fulfillConditions(String operatorId) {
        this.conditionsFulfilled = true;
        this.conditionsConfirmedBy = operatorId;
    }
}
