package com.hanati.bank.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_CREDIT_INFO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CREDIT_ID")
    private Long creditId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "CREDIT_SCORE", nullable = false)
    private Integer creditScore;

    @Column(name = "ANNUAL_INCOME", nullable = false)
    private Long annualIncome;

    @Column(name = "EMPLOYMENT_TYPE", length = 30)
    private String employmentType;

    @Column(name = "COMPANY_NAME", length = 100)
    private String companyName;

    @Column(name = "EMPLOYMENT_MONTHS")
    private Integer employmentMonths;

    @Column(name = "EXISTING_DEBT")
    @Builder.Default
    private Long existingDebt = 0L;

    @Column(name = "ANNUAL_REPAYMENT")
    @Builder.Default
    private Long annualRepayment = 0L;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
