package com.hanati.bank.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "LOAN_PRODUCT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 100)
    private String productName;

    @Column(name = "PRODUCT_TYPE", nullable = false, length = 30)
    private String productType;

    @Column(name = "MIN_INTEREST_RATE", precision = 5, scale = 2)
    private BigDecimal minInterestRate;

    @Column(name = "MAX_INTEREST_RATE", precision = 5, scale = 2)
    private BigDecimal maxInterestRate;

    @Column(name = "MAX_LIMIT_AMOUNT")
    private Long maxLimitAmount;

    @Column(name = "LOAN_PERIOD_MONTHS")
    private Integer loanPeriodMonths;

    @Column(name = "USE_YN", length = 1)
    @Builder.Default
    private String useYn = "Y";
}
