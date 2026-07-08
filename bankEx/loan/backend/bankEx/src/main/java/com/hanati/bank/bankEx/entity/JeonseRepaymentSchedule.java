package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_REPAYMENT_SCHEDULE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEDULE_ID")
    private Long scheduleId;

    @Column(name = "APPLICATION_ID", nullable = false, length = 40)
    private String applicationId;

    @Column(name = "PAYMENT_SEQ", nullable = false)
    private Integer paymentSeq;

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "PRINCIPAL_AMOUNT")
    private Long principalAmount;

    @Column(name = "INTEREST_AMOUNT")
    private Long interestAmount;

    @Column(name = "TOTAL_AMOUNT")
    private Long totalAmount;

    @Setter
    @Column(name = "PAID_YN", length = 1)
    private String paidYn;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
