package com.hanati.bank.refinance.refinance.entity;

import com.hanati.bank.refinance.refinance.domain.ErrorProcessStatus;
import com.hanati.bank.refinance.refinance.domain.FailedStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_REFINANCE_ERROR")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ERROR_ID")
    private Long errorId;

    @Column(name = "APPLICATION_ID")
    private Long applicationId;

    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FAILED_STEP")
    private FailedStep failedStep;

    @Column(name = "ERROR_CODE")
    private String errorCode;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private ErrorProcessStatus status;

    @Setter
    @Column(name = "RETRY_COUNT")
    private Integer retryCount;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
