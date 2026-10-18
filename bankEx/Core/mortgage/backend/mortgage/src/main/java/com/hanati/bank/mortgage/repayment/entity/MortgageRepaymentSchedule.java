package com.hanati.bank.mortgage.repayment.entity;

import com.hanati.bank.mortgage.repayment.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "MORTGAGE_REPAYMENT_SCHEDULE",
        uniqueConstraints = @UniqueConstraint(columnNames = {"LOAN_ACCOUNT_ID", "INSTALLMENT_NO"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEDULE_ID")
    private Long scheduleId;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Column(name = "INSTALLMENT_NO", nullable = false)
    private Integer installmentNo;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "PRINCIPAL", nullable = false)
    private Long principal;

    @Column(name = "INTEREST", nullable = false)
    private Long interest;

    @Column(name = "TOTAL_PAYMENT", nullable = false)
    private Long totalPayment;

    @Column(name = "BALANCE_AFTER", nullable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ScheduleStatus status;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    public void markPaid(LocalDateTime now) {
        this.status = ScheduleStatus.PAID;
        this.paidAt = now;
    }
}
