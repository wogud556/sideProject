package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회차별 납부예정금액과 납부 결과 (명세 6.2).
 *
 * <p>정상 원금·이자는 scheduled - paid 가 미납 잔액이고, 연체 원금·이자는 잔액을 직접 보관한다.
 * 연체 전이 로직이 범위 밖이므로, 한 회차에서 정상 버킷과 연체 버킷은 시드 단계에서
 * 상호배타적으로 설정하는 것을 전제한다.
 */
@Entity
@Table(name = "REPAYMENT_SCHEDULE",
        uniqueConstraints = @UniqueConstraint(name = "UK_SCHEDULE_ACCOUNT_SEQ",
                columnNames = {"LOAN_ACCOUNT_ID", "INSTALLMENT_NUMBER"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Column(name = "INSTALLMENT_NUMBER", nullable = false)
    private Integer installmentNumber;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "SCHEDULED_PRINCIPAL", nullable = false, precision = 18, scale = 0)
    private BigDecimal scheduledPrincipal;

    @Column(name = "SCHEDULED_INTEREST", nullable = false, precision = 18, scale = 0)
    private BigDecimal scheduledInterest;

    @Setter
    @Column(name = "PAID_PRINCIPAL", nullable = false, precision = 18, scale = 0)
    private BigDecimal paidPrincipal;

    @Setter
    @Column(name = "PAID_INTEREST", nullable = false, precision = 18, scale = 0)
    private BigDecimal paidInterest;

    @Setter
    @Column(name = "OVERDUE_PRINCIPAL", nullable = false, precision = 18, scale = 0)
    private BigDecimal overduePrincipal;

    @Setter
    @Column(name = "OVERDUE_INTEREST", nullable = false, precision = 18, scale = 0)
    private BigDecimal overdueInterest;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ScheduleStatus status;

    @Setter
    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    public BigDecimal outstandingPrincipal() {
        return scheduledPrincipal.subtract(paidPrincipal);
    }

    public BigDecimal outstandingInterest() {
        return scheduledInterest.subtract(paidInterest);
    }

    public BigDecimal totalOutstanding() {
        return outstandingPrincipal().add(outstandingInterest())
                .add(overduePrincipal).add(overdueInterest);
    }
}
