package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "LOAN_PRODUCT")
@Getter
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

    @Column(name = "INTEREST_RATE")
    private Double interestRate;

    @Column(name = "MAX_LIMIT")
    private Long maxLimit;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
}
