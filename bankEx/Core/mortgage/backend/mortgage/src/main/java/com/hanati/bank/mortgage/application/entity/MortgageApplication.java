package com.hanati.bank.mortgage.application.entity;

import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.enums.ApplicationStatusTransition;
import com.hanati.bank.mortgage.application.enums.BorrowerType;
import com.hanati.bank.mortgage.application.enums.LoanPurpose;
import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 주담대 신청 원장. 실제 대출 계좌(MortgageLoanAccount)와는 분리한다. */
@Entity
@Table(name = "MORTGAGE_APPLICATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPLICATION_ID")
    private Long applicationId;

    @Column(name = "APPLICATION_NO", unique = true, length = 30)
    private String applicationNo;

    /** bankEx 고객 ID (USER_INFO.USER_ID) */
    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "CUSTOMER_NAME", nullable = false, length = 50)
    private String customerName;

    @Column(name = "PRODUCT_CODE", nullable = false, length = 20)
    private String productCode;

    @Column(name = "REQUESTED_AMOUNT", nullable = false)
    private Long requestedAmount;

    @Column(name = "TERM_MONTHS", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPAYMENT_METHOD", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_PURPOSE", nullable = false, length = 20)
    private LoanPurpose loanPurpose;

    /** 신청 시점 보유 주택 수 (담보 주택 구입 전 기준) */
    @Column(name = "HOUSE_COUNT", nullable = false)
    private Integer houseCount;

    @Column(name = "FIRST_HOME_BUYER", nullable = false)
    private Boolean firstHomeBuyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "APPLIED_AT", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public void assignApplicationNo(String applicationNo) {
        this.applicationNo = applicationNo;
    }

    public void changeStatus(ApplicationStatus next, LocalDateTime now) {
        ApplicationStatusTransition.validate(this.status, next);
        this.status = next;
        this.updatedAt = now;
    }

    /** 다음 상태로 갈 수 있는지만 확인한다. 상태를 바꾸기 전에 다른 검증을 먼저 해야 할 때 쓴다. */
    public void requireTransitionTo(ApplicationStatus next) {
        ApplicationStatusTransition.validate(this.status, next);
    }

    public BorrowerType borrowerType() {
        if (houseCount >= 2) {
            return BorrowerType.MULTI_HOME;
        }
        if (houseCount == 1) {
            return BorrowerType.ONE_HOME;
        }
        return Boolean.TRUE.equals(firstHomeBuyer) ? BorrowerType.FIRST_HOME : BorrowerType.NO_HOME;
    }
}
