package com.hanati.bank.mortgage.repayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 실제 상환 거래. 출금은 bankEx가 먼저 하고, 그 거래번호를 멱등 키로 받는다. */
@Entity
@Table(name = "MORTGAGE_REPAYMENT_HISTORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageRepaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPAYMENT_ID")
    private Long repaymentId;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Column(name = "INSTALLMENT_NO", nullable = false)
    private Integer installmentNo;

    @Column(name = "PRINCIPAL_PAID", nullable = false)
    private Long principalPaid;

    @Column(name = "INTEREST_PAID", nullable = false)
    private Long interestPaid;

    @Column(name = "TOTAL_PAID", nullable = false)
    private Long totalPaid;

    @Column(name = "OUTSTANDING_AFTER", nullable = false)
    private Long outstandingAfter;

    @Column(name = "SOURCE_ACCOUNT_NO", nullable = false, length = 20)
    private String sourceAccountNo;

    /** bankEx 출금 거래번호. 같은 출금으로 두 번 상환 처리되는 것을 막는다 */
    @Column(name = "IDEMPOTENCY_KEY", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "PAID_AT", nullable = false)
    private LocalDateTime paidAt;
}
