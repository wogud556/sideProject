package com.hanati.bank.mortgage.collateral.entity;

import com.hanati.bank.mortgage.collateral.enums.ValuationMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 담보 평가 이력. 재평가해도 이전 행을 지우지 않고, 한도 산정은 가장 최근 평가를 쓴다. */
@Entity
@Table(name = "MORTGAGE_VALUATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VALUATION_ID")
    private Long valuationId;

    @Column(name = "PROPERTY_ID", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "VALUATION_METHOD", nullable = false, length = 20)
    private ValuationMethod valuationMethod;

    @Column(name = "APPRAISED_VALUE", nullable = false)
    private Long appraisedValue;

    @Column(name = "VALUATION_DATE", nullable = false)
    private LocalDate valuationDate;

    @Column(name = "VALUED_BY", nullable = false, length = 20)
    private String valuedBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
