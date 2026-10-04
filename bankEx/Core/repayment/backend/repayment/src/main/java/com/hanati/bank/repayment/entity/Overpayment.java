package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.OverpaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 과오납 보관 및 환급 상태 (명세 3.8). */
@Entity
@Table(name = "OVERPAYMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Overpayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Column(name = "REPAYMENT_TRANSACTION_ID", nullable = false)
    private Long repaymentTransactionId;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private OverpaymentStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "REFUNDED_AT")
    private LocalDateTime refundedAt;

    @Setter
    @Column(name = "REFUND_FAIL_REASON", length = 200)
    private String refundFailReason;
}
