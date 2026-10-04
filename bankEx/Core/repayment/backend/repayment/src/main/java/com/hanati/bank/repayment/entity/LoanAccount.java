package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.LoanAccountStatus;
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
 * 대출계좌의 현재 채무 잔액 (명세 6.1).
 *
 * <p>금액은 모두 원화 기준이므로 scale 0 BigDecimal로 보관한다. 연체 전이 로직(정상 → 연체 이전)과
 * 연체이자 산정은 이번 범위에서 제외했으므로, overduePrincipal / overdueInterest / feeBalance는
 * 시드 또는 테스트에서 직접 설정한 값을 상환·취소가 증감시키기만 한다.
 */
@Entity
@Table(name = "LOAN_ACCOUNT",
        uniqueConstraints = @UniqueConstraint(name = "UK_LOAN_ACCOUNT_NUMBER", columnNames = "LOAN_ACCOUNT_NUMBER"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LOAN_ACCOUNT_NUMBER", nullable = false, length = 30)
    private String loanAccountNumber;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    /** 배분 정책 선택 키 (명세 4번: 상품 또는 상품 유형에 따라 정책을 고른다). */
    @Column(name = "PRODUCT_TYPE", nullable = false, length = 30)
    private String productType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private LoanAccountStatus status;

    @Column(name = "ORIGINAL_PRINCIPAL", nullable = false, precision = 18, scale = 0)
    private BigDecimal originalPrincipal;

    @Setter
    @Column(name = "PRINCIPAL_BALANCE", nullable = false, precision = 18, scale = 0)
    private BigDecimal principalBalance;

    @Setter
    @Column(name = "OVERDUE_PRINCIPAL", nullable = false, precision = 18, scale = 0)
    private BigDecimal overduePrincipal;

    @Setter
    @Column(name = "ACCRUED_INTEREST", nullable = false, precision = 18, scale = 0)
    private BigDecimal accruedInterest;

    @Setter
    @Column(name = "OVERDUE_INTEREST", nullable = false, precision = 18, scale = 0)
    private BigDecimal overdueInterest;

    @Setter
    @Column(name = "FEE_BALANCE", nullable = false, precision = 18, scale = 0)
    private BigDecimal feeBalance;

    /** 중도상환수수료율(%). 0이면 수수료를 받지 않는다. */
    @Column(name = "PREPAYMENT_FEE_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal prepaymentFeeRate;

    @Column(name = "DISBURSEMENT_DATE", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "MATURITY_DATE", nullable = false)
    private LocalDate maturityDate;

    @Setter
    @Column(name = "LAST_TRANSACTION_DATE")
    private LocalDateTime lastTransactionDate;

    /** 자동이체 출금계좌 (명세 3.2). */
    @Column(name = "WITHDRAWAL_ACCOUNT_NUMBER", length = 30)
    private String withdrawalAccountNumber;

    @Version
    @Column(name = "VERSION")
    private Long version;

    /** 잔존 채무 합계. 0이면 완제 대상이다. */
    public BigDecimal totalOutstanding() {
        return principalBalance
                .add(overduePrincipal)
                .add(accruedInterest)
                .add(overdueInterest)
                .add(feeBalance);
    }

    public boolean isRepayable() {
        return status == LoanAccountStatus.ACTIVE
                || status == LoanAccountStatus.PAST_DUE
                || status == LoanAccountStatus.DELINQUENT;
    }
}
