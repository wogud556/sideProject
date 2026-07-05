package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DEPOSIT_TRANSACTION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "BALANCE_AFTER", nullable = false)
    private Long balanceAfter;

    @Column(name = "DESCRIPTION", length = 300)
    private String description;

    @Column(name = "TRANSACTION_AT", nullable = false)
    private LocalDateTime transactionAt;
}
