package com.hanati.bank.refinance.loan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_LOAN")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExistingLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    private Long customerId;

    private String financialInstitutionCode;
    private String financialInstitutionName;
    private String loanAccountNo;
    private String loanProductCode;
    private String loanProductName;
    private String loanType;

    private BigDecimal originalAmount;

    @Setter
    private BigDecimal currentBalance;

    private BigDecimal interestRate;
    private LocalDate executionDate;
    private LocalDate maturityDate;
    private String repaymentMethod;

    private String overdueYn;

    @Setter
    private String status;
}
