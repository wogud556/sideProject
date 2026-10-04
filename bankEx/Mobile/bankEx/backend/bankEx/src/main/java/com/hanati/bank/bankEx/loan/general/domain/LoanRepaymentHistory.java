package com.hanati.bank.bankEx.loan.general.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanRepaymentHistory {
    private Long historyId;
    private Long applicationId;
    private Integer paymentSeq;
    private String repaymentType;
    private Long totalPayment;
    private Long principalAmount;
    private Long interestAmount;
    private Long remainingBalance;
    private LocalDateTime paidAt;
}
