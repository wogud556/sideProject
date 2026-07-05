package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPLICATION_ID")
    private Long applicationId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "REQUEST_AMOUNT", nullable = false)
    private Long requestAmount;

    @Column(name = "LOAN_PERIOD", nullable = false)
    private Integer loanPeriod;

    @Setter
    @Column(name = "STATUS", length = 20)
    private String status;

    @Setter
    @Column(name = "REMAINING_BALANCE")
    private Long remainingBalance;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
