package com.hanati.bank.bankEx.deposit.general.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "ACCOUNT_NUMBER", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "PRODUCT_CODE", length = 20)
    private String productCode;

    @Column(name = "ACCOUNT_NAME", length = 100)
    private String accountName;

    @Setter
    @Column(name = "BALANCE")
    private Long balance;

    @Setter
    @Column(name = "ACCOUNT_STATUS", length = 20)
    private String accountStatus;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Setter
    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @Column(name = "ACCOUNT_PASSWORD", length = 255)
    private String accountPassword;

    @Column(name = "PER_TRANSFER_LIMIT")
    private Long perTransferLimit;

    @Column(name = "DAILY_TRANSFER_LIMIT")
    private Long dailyTransferLimit;
}
