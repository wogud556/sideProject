package com.hanati.bank.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_APPLICATION")
@Getter
@Setter
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

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "REQUEST_AMOUNT", nullable = false)
    private Long requestAmount;

    @Column(name = "APPLICATION_STATUS", length = 30)
    @Builder.Default
    private String applicationStatus = "APPLIED";

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
