package com.hanati.bank.screening.entity;

import com.hanati.bank.screening.enums.LoanType;
import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_SCREENING")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCREENING_ID")
    private Long screeningId;

    // ── 심사 요청 (§9.3) ──
    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "LOAN_PRODUCT_ID", nullable = false, length = 50)
    private String loanProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_TYPE", nullable = false, length = 20)
    private LoanType loanType;

    @Column(name = "REQUESTED_AMOUNT", nullable = false)
    private Long requestedAmount;

    @Column(name = "ANNUAL_INCOME", nullable = false)
    private Long annualIncome;

    @Column(name = "CREDIT_SCORE", nullable = false)
    private Integer creditScore;

    @Column(name = "EXISTING_LOAN_AMOUNT", nullable = false)
    private Long existingLoanAmount;

    // ── 심사 결과 ──
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ScreeningStatus status;

    @Column(name = "APPROVED_AMOUNT")
    private Long approvedAmount;

    @Column(name = "APPROVED_INTEREST_RATE", precision = 5, scale = 2)
    private BigDecimal approvedInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "REASON_CODE", nullable = false, length = 40)
    private ScreeningReasonCode reasonCode;

    @Column(name = "SCREENED_AT", nullable = false)
    private LocalDateTime screenedAt;

    // ── 심사역 결정 (MANUAL_REVIEW 건만) ──
    @Column(name = "REVIEWER_ID", length = 50)
    private String reviewerId;

    @Column(name = "REVIEW_COMMENT", length = 500)
    private String reviewComment;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    public void approveByReviewer(String reviewerId, long approvedAmount, BigDecimal interestRate,
                                  String comment, LocalDateTime now) {
        this.status = ScreeningStatus.APPROVED;
        this.approvedAmount = approvedAmount;
        this.approvedInterestRate = interestRate;
        this.reasonCode = ScreeningReasonCode.REVIEWER_APPROVED;
        recordReview(reviewerId, comment, now);
    }

    public void rejectByReviewer(String reviewerId, String comment, LocalDateTime now) {
        this.status = ScreeningStatus.REJECTED;
        this.reasonCode = ScreeningReasonCode.REVIEWER_REJECTED;
        recordReview(reviewerId, comment, now);
    }

    private void recordReview(String reviewerId, String comment, LocalDateTime now) {
        this.reviewerId = reviewerId;
        this.reviewComment = comment;
        this.reviewedAt = now;
    }
}
