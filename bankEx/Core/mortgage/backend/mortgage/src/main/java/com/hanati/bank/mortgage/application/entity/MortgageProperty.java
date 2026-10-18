package com.hanati.bank.mortgage.application.entity;

import com.hanati.bank.mortgage.application.enums.PropertyType;
import com.hanati.bank.mortgage.application.enums.RegionCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 담보 부동산. 신청 1건에 1건이다. 평가 이력과 권리관계는 collateral 모듈이 관리한다. */
@Entity
@Table(name = "MORTGAGE_PROPERTY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROPERTY_ID")
    private Long propertyId;

    @Column(name = "APPLICATION_ID", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "ADDRESS", nullable = false, length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "REGION_CODE", nullable = false, length = 20)
    private RegionCode regionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROPERTY_TYPE", nullable = false, length = 20)
    private PropertyType propertyType;

    @Column(name = "EXCLUSIVE_AREA", precision = 7, scale = 2)
    private BigDecimal exclusiveArea;

    /** 고객이 제시한 매매가 또는 시세. 담보가치는 평가액(MortgageValuation)으로 판단한다 */
    @Column(name = "DECLARED_PRICE", nullable = false)
    private Long declaredPrice;
}
