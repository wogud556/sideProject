package com.hanati.bank.mortgage.collateral.entity;

import com.hanati.bank.mortgage.collateral.enums.LienType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 선순위 채권·임차보증금·당행 담보권. 근저당은 채권최고액, 임차는 보증금을 AMOUNT에 담는다. */
@Entity
@Table(name = "MORTGAGE_LIEN")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageLien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LIEN_ID")
    private Long lienId;

    @Column(name = "PROPERTY_ID", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LIEN_TYPE", nullable = false, length = 20)
    private LienType lienType;

    @Column(name = "CREDITOR", nullable = false, length = 50)
    private String creditor;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "LIEN_RANK", nullable = false)
    private Integer lienRank;

    @Column(name = "REGISTERED_BY", nullable = false, length = 20)
    private String registeredBy;

    @Column(name = "REGISTERED_AT", nullable = false)
    private LocalDateTime registeredAt;
}
