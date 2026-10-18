package com.hanati.bank.mortgage.application.entity;

import com.hanati.bank.mortgage.application.enums.IncomeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MORTGAGE_INCOME")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INCOME_ID")
    private Long incomeId;

    @Column(name = "APPLICATION_ID", nullable = false, unique = true)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCOME_TYPE", nullable = false, length = 20)
    private IncomeType incomeType;

    @Column(name = "ANNUAL_INCOME", nullable = false)
    private Long annualIncome;

    @Column(name = "EMPLOYER_NAME", length = 100)
    private String employerName;
}
