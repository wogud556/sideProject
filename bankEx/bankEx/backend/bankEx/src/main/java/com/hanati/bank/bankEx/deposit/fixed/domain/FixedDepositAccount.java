package com.hanati.bank.bankEx.deposit.fixed.domain;

import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDepositAccount {
    private String depositAccountId;
    private String userId;
    private String depositProductId;
    private String withdrawAccountNo;
    private Long principalAmount;
    private Double interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private Long expectedInterest;
    private Long expectedMaturityAmount;

    @Setter
    private FixedDepositStatus status;

    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;
}
