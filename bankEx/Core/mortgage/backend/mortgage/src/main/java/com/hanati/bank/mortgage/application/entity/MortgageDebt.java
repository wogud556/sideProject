package com.hanati.bank.mortgage.application.entity;

import com.hanati.bank.mortgage.application.enums.DebtType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기존 부채. DSR은 연간 원리금, 신DTI는 주담대 원리금 + 기타 부채 이자를 쓴다. */
@Entity
@Table(name = "MORTGAGE_DEBT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageDebt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEBT_ID")
    private Long debtId;

    @Column(name = "APPLICATION_ID", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "DEBT_TYPE", nullable = false, length = 20)
    private DebtType debtType;

    @Column(name = "INSTITUTION", length = 50)
    private String institution;

    @Column(name = "BALANCE", nullable = false)
    private Long balance;

    @Column(name = "ANNUAL_PRINCIPAL", nullable = false)
    private Long annualPrincipal;

    @Column(name = "ANNUAL_INTEREST", nullable = false)
    private Long annualInterest;

    public long annualDebtService() {
        return annualPrincipal + annualInterest;
    }
}
