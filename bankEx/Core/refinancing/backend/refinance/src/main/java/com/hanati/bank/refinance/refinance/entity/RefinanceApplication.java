package com.hanati.bank.refinance.refinance.entity;

import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatusTransition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_REFINANCE_APPLICATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPLICATION_ID")
    private Long applicationId;

    @Column(name = "APPLICATION_NO")
    private String applicationNo;
    @Column(name = "CUSTOMER_ID")
    private Long customerId;
    private LocalDateTime applicationDate;

    @Setter
    @Enumerated(EnumType.STRING)
    private RefinanceStatus status;

    private BigDecimal requestedAmount;

    @Setter
    private BigDecimal approvedAmount;

    // 신규대출 조건 (명세 10번)
    @Setter
    private String newLoanProductName;
    @Setter
    private BigDecimal newLoanAmount;
    @Setter
    private BigDecimal newLoanRate;
    @Setter
    private String newLoanRateType;
    @Setter
    private Integer newLoanPeriodMonths;
    @Setter
    private LocalDate newLoanMaturityDate;
    @Setter
    private String newLoanRepaymentMethod;
    @Setter
    private LocalDate newLoanExecutionScheduledDate;
    @Setter
    private String newLoanAccountNo;
    @Setter
    private String refinancePurposeYn;

    @Setter
    private String rejectReasonCode;

    private String createdBy;
    private LocalDateTime createdAt;

    @Setter
    private String updatedBy;
    @Setter
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void changeStatus(RefinanceStatus newStatus, String operatorId) {
        if (!RefinanceStatusTransition.isAllowed(this.status, newStatus)) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        this.status = newStatus;
        this.updatedBy = operatorId;
        this.updatedAt = LocalDateTime.now();
    }
}
