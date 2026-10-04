package com.hanati.bank.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_SCREENING_RESULT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCREENING_ID")
    private Long screeningId;

    @Column(name = "APPLICATION_ID", nullable = false)
    private Long applicationId;

    @Column(name = "CSS_SCORE")
    private Integer cssScore;

    @Column(name = "DSR_RATE", precision = 5, scale = 2)
    private BigDecimal dsrRate;

    @Column(name = "APPROVED_AMOUNT")
    private Long approvedAmount;

    @Column(name = "INTEREST_RATE", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "RESULT_STATUS", length = 30)
    private String resultStatus;

    @Column(name = "REJECT_REASON", length = 500)
    private String rejectReason;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
