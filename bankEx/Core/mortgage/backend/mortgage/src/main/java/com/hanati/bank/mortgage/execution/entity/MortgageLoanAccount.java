package com.hanati.bank.mortgage.execution.entity;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import com.hanati.bank.mortgage.execution.enums.LoanAccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 실행된 주담대 계좌. 약정 조건을 복사해 확정한다. */
@Entity
@Table(name = "MORTGAGE_LOAN_ACCOUNT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageLoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOAN_ACCOUNT_ID")
    private Long loanAccountId;

    @Column(name = "LOAN_ACCOUNT_NO", unique = true, length = 20)
    private String loanAccountNo;

    @Column(name = "APPLICATION_ID", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "PRODUCT_CODE", nullable = false, length = 20)
    private String productCode;

    @Column(name = "PRINCIPAL", nullable = false)
    private Long principal;

    @Column(name = "OUTSTANDING_PRINCIPAL", nullable = false)
    private Long outstandingPrincipal;

    @Column(name = "INTEREST_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "TERM_MONTHS", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPAYMENT_METHOD", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Column(name = "EXECUTION_DATE", nullable = false)
    private LocalDate executionDate;

    @Column(name = "MATURITY_DATE", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private LoanAccountStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public void assignLoanAccountNo(String loanAccountNo) {
        this.loanAccountNo = loanAccountNo;
    }

    public void repayPrincipal(long amount) {
        this.outstandingPrincipal -= amount;
    }

    public void close() {
        this.status = LoanAccountStatus.CLOSED;
    }
}
