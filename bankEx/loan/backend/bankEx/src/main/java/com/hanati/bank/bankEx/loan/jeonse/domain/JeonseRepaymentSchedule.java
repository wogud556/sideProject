package com.hanati.bank.bankEx.loan.jeonse.domain;

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
public class JeonseRepaymentSchedule {
    private Long scheduleId;
    private String applicationId;
    private Integer paymentSeq;
    private LocalDate paymentDate;
    private Long principalAmount;
    private Long interestAmount;
    private Long totalAmount;

    @Setter
    private String paidYn;

    private LocalDateTime createdAt;
}
