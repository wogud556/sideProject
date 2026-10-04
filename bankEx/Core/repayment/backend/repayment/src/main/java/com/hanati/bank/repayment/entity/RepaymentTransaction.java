package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.LoanAccountStatus;
import com.hanati.bank.repayment.enums.PaymentMethod;
import com.hanati.bank.repayment.enums.RepaymentTransactionStatus;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 상환 요청 한 건의 거래 헤더 (명세 6.3). 완료된 거래는 수정하지 않고 역거래로만 되돌린다. */
@Entity
@Table(name = "REPAYMENT_TRANSACTION",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_REPAYMENT_TXN_NUMBER", columnNames = "TRANSACTION_NUMBER"),
                @UniqueConstraint(name = "UK_REPAYMENT_IDEMPOTENCY_KEY", columnNames = "IDEMPOTENCY_KEY")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TRANSACTION_NUMBER", nullable = false, length = 30)
    private String transactionNumber;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 30)
    private RepaymentTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "REQUESTED_AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal requestedAmount;

    @Column(name = "ALLOCATED_AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal allocatedAmount;

    @Column(name = "OVERPAYMENT_AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal overpaymentAmount;

    /**
     * 이 거래에서 새로 발생시킨 중도상환수수료. FEE 배분액에는 기존 비용잔액과 이 수수료가 함께 들어가므로,
     * 취소 시 기존 비용잔액만 되살리기 위해 따로 보관한다.
     */
    @Column(name = "PREPAYMENT_FEE", nullable = false, precision = 18, scale = 0)
    private BigDecimal prepaymentFee;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private RepaymentTransactionStatus status;

    @Column(name = "BUSINESS_DATE", nullable = false)
    private LocalDate businessDate;

    @Column(name = "PROCESSED_AT", nullable = false)
    private LocalDateTime processedAt;

    /** 멱등성 키. 고유 제약으로 중복 처리를 막는다 (명세 10번). */
    @Column(name = "IDEMPOTENCY_KEY", nullable = false, length = 100)
    private String idempotencyKey;

    /** 역거래인 경우 원거래 ID, 원거래가 취소된 경우 취소거래 ID. */
    @Setter
    @Column(name = "ORIGINAL_TRANSACTION_ID")
    private Long originalTransactionId;

    /**
     * 거래 직전의 대출계좌 상태. 취소 시 상태를 추측하지 않고 그대로 복원하기 위해 보관한다
     * (명세 3.7의 "대출계좌 상태 복원").
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_STATUS_BEFORE", length = 20)
    private LoanAccountStatus accountStatusBefore;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "REASON", length = 200)
    private String reason;
}
