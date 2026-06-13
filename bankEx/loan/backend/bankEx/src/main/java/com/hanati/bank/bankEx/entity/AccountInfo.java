package com.hanati.bank.bankEx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "BALANCE")
    private Long balance;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
