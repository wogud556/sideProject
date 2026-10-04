package com.hanati.bank.bankEx.deposit.transfer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_TRANSFER")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSFER_ID")
    private Long transferId;

    @Column(name = "REQUEST_ID", unique = true, nullable = false, length = 50)
    private String requestId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "WITHDRAWAL_ACCOUNT_ID", nullable = false)
    private Long withdrawalAccountId;

    @Column(name = "WITHDRAWAL_ACCOUNT_NUMBER", nullable = false, length = 20)
    private String withdrawalAccountNumber;

    @Column(name = "DEPOSIT_ACCOUNT_ID", nullable = false)
    private Long depositAccountId;

    @Column(name = "DEPOSIT_ACCOUNT_NUMBER", nullable = false, length = 20)
    private String depositAccountNumber;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "WITHDRAWAL_MEMO", length = 100)
    private String withdrawalMemo;

    @Column(name = "DEPOSIT_MEMO", length = 100)
    private String depositMemo;

    @Setter
    @Column(name = "TRANSACTION_NUMBER", length = 50)
    private String transactionNumber;

    @Setter
    @Column(name = "BALANCE_AFTER_TRANSFER")
    private Long balanceAfterTransfer;

    @Setter
    @Column(name = "TRANSFER_STATUS", nullable = false, length = 20)
    private String transferStatus;

    @Column(name = "FAILURE_REASON", length = 500)
    private String failureReason;

    @Setter
    @Column(name = "TRANSFERRED_AT")
    private LocalDateTime transferredAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
