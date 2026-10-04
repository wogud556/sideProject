package com.hanati.bank.refinance.refinance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_REFINANCE_TARGET")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long targetId;

    private Long applicationId;
    private Long loanId;

    private String financialInstitutionCode;
    private String loanAccountNo;
    private String loanProductCode;

    private BigDecimal loanBalance;
    private BigDecimal repaymentAmount;
    private BigDecimal prepaymentFee;
    private BigDecimal interestAmount;
}
