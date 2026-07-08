package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "JEONSE_LOAN_PRODUCT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseLoanProduct {

    @Id
    @Column(name = "PRODUCT_ID", length = 30)
    private String productId;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 100)
    private String productName;

    @Column(name = "PRODUCT_TYPE", nullable = false, length = 30)
    private String productType;

    @Column(name = "MAX_LIMIT_AMOUNT", nullable = false)
    private Long maxLimitAmount;

    @Column(name = "BASE_RATE", nullable = false)
    private Double baseRate;

    @Column(name = "MIN_RATE")
    private Double minRate;

    @Column(name = "MAX_RATE")
    private Double maxRate;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
