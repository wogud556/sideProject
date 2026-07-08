package com.hanati.bank.bankEx.entity;

import com.hanati.bank.bankEx.enums.GuaranteeOrg;
import com.hanati.bank.bankEx.enums.JeonseLoanStatus;
import com.hanati.bank.bankEx.enums.RejectReasonCode;
import com.hanati.bank.bankEx.enums.RepaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "JEONSE_LOAN_APPLICATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseLoanApplication {

    @Id
    @Column(name = "APPLICATION_ID", length = 40)
    private String applicationId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "PRODUCT_ID", nullable = false, length = 30)
    private String productId;

    @Column(name = "REQUEST_AMOUNT", nullable = false)
    private Long requestAmount;

    @Setter
    @Column(name = "AVAILABLE_LIMIT_AMOUNT")
    private Long availableLimitAmount;

    @Setter
    @Column(name = "APPROVED_AMOUNT")
    private Long approvedAmount;

    @Column(name = "ANNUAL_INCOME", nullable = false)
    private Long annualIncome;

    @Column(name = "EXISTING_DEBT_AMOUNT")
    private Long existingDebtAmount;

    @Column(name = "CREDIT_SCORE", nullable = false)
    private Integer creditScore;

    @Column(name = "HOMELESS_YN", nullable = false, length = 1)
    private String homelessYn;

    @Column(name = "HOUSEHOLDER_YN", nullable = false, length = 1)
    private String householderYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "GUARANTEE_ORG", nullable = false, length = 10)
    private GuaranteeOrg guaranteeOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPAYMENT_TYPE", nullable = false, length = 30)
    private RepaymentType repaymentType;

    @Setter
    @Column(name = "LOAN_RATE")
    private Double loanRate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private JeonseLoanStatus status;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "REJECT_REASON_CODE", length = 50)
    private RejectReasonCode rejectReasonCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
