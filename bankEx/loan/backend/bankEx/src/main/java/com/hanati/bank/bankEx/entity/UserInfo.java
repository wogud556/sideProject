package com.hanati.bank.bankEx.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    @Id
    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "USER_NAME", nullable = false)
    private String userName;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
