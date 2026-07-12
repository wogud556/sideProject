package com.hanati.bank.bankEx.deposit.savings.domain;

import com.hanati.bank.bankEx.deposit.savings.enums.SavingsAccountStatus;
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
public class SavingsAccount {
    private String accountNo;
    private String userId;
    private String productId;
    private String withdrawAccountNo;
    private Long monthlyAmount;
    private Integer period;

    @Setter
    private Integer currentCount;

    @Setter
    private Long balance;

    private Double interestRate;

    @Setter
    private SavingsAccountStatus status;

    private LocalDate openDate;
    private LocalDate maturityDate;
    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime updatedAt;
}
