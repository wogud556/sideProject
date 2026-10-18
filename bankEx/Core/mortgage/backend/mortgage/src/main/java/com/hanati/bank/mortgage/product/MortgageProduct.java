package com.hanati.bank.mortgage.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "MORTGAGE_PRODUCT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageProduct {

    @Id
    @Column(name = "PRODUCT_CODE", length = 20)
    private String productCode;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 100)
    private String productName;

    @Column(name = "MIN_AMOUNT", nullable = false)
    private Long minAmount;

    /** 상품 한도. 한도 산정에서 productLimit으로 쓰인다 */
    @Column(name = "MAX_AMOUNT", nullable = false)
    private Long maxAmount;

    @Column(name = "MIN_TERM_MONTHS", nullable = false)
    private Integer minTermMonths;

    @Column(name = "MAX_TERM_MONTHS", nullable = false)
    private Integer maxTermMonths;

    @Column(name = "BASE_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "DESCRIPTION", length = 300)
    private String description;
}
