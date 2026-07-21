package com.hanati.bank.bankEx.loan.general.domain;

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
public class LoanApplication {
    private Long applicationId;
    private String userId;
    private String accountNumber;
    private Long productId;
    private Long requestAmount;
    private Integer loanPeriod;

    @Setter
    private String status;

    @Setter
    private Long remainingBalance;

    @Setter
    private String repaymentType;

    private LocalDateTime createdAt;
}
