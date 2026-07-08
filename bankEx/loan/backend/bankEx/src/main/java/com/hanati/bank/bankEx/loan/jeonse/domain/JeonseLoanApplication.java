package com.hanati.bank.bankEx.loan.jeonse.domain;

import com.hanati.bank.bankEx.loan.jeonse.enums.GuaranteeOrg;
import com.hanati.bank.bankEx.loan.jeonse.enums.JeonseLoanStatus;
import com.hanati.bank.bankEx.loan.jeonse.enums.RejectReasonCode;
import com.hanati.bank.bankEx.loan.jeonse.enums.RepaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseLoanApplication {
    private String applicationId;
    private String userId;
    private String productId;
    private Long requestAmount;

    @Setter
    private Long availableLimitAmount;

    @Setter
    private Long approvedAmount;

    private Long annualIncome;
    private Long existingDebtAmount;
    private Integer creditScore;
    private String homelessYn;
    private String householderYn;
    private GuaranteeOrg guaranteeOrg;
    private RepaymentType repaymentType;

    @Setter
    private Double loanRate;

    @Setter
    private JeonseLoanStatus status;

    @Setter
    private RejectReasonCode rejectReasonCode;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;
}
