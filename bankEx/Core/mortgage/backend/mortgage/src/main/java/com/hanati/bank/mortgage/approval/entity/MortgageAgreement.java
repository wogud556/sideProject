package com.hanati.bank.mortgage.approval.entity;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 대출 약정. 고객이 승인 조건에 동의하고 실행금을 받을 계좌를 지정한다. */
@Entity
@Table(name = "MORTGAGE_AGREEMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AGREEMENT_ID")
    private Long agreementId;

    @Column(name = "AGREEMENT_NO", unique = true, length = 30)
    private String agreementNo;

    @Column(name = "APPLICATION_ID", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "APPROVAL_ID", nullable = false)
    private Long approvalId;

    /** bankEx 수신계좌 번호 */
    @Column(name = "DISBURSEMENT_ACCOUNT_NO", nullable = false, length = 20)
    private String disbursementAccountNo;

    @Column(name = "AGREED_AMOUNT", nullable = false)
    private Long agreedAmount;

    @Column(name = "INTEREST_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "TERM_MONTHS", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPAYMENT_METHOD", nullable = false, length = 20)
    private RepaymentMethod repaymentMethod;

    @Column(name = "AGREED_AT", nullable = false)
    private LocalDateTime agreedAt;

    // ── 담보권(당행 근저당) 설정 확인 ──
    @Column(name = "LIEN_ID")
    private Long lienId;

    @Column(name = "LIEN_CONFIRMED_BY", length = 20)
    private String lienConfirmedBy;

    @Column(name = "LIEN_CONFIRMED_AT")
    private LocalDateTime lienConfirmedAt;

    public void assignAgreementNo(String agreementNo) {
        this.agreementNo = agreementNo;
    }

    public boolean isLienRegistered() {
        return lienId != null;
    }

    public void confirmLien(Long lienId, String operatorId, LocalDateTime now) {
        this.lienId = lienId;
        this.lienConfirmedBy = operatorId;
        this.lienConfirmedAt = now;
    }
}
