package com.hanati.bank.bankEx.deposit.savings.domain;

import com.hanati.bank.bankEx.deposit.savings.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPaymentHistory {
    private Long paymentId;
    private String accountNo;
    private Integer paymentSeq;
    private LocalDate paymentDate;
    private Long paymentAmount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
