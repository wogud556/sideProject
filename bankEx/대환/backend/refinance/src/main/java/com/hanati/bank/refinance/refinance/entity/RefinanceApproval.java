package com.hanati.bank.refinance.refinance.entity;

import com.hanati.bank.refinance.refinance.domain.ApprovalDecision;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_REFINANCE_APPROVAL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;

    private String approverId;
    private LocalDateTime approvedAt;
    private BigDecimal approvedAmount;
    private String approvalCondition;
    private String approvalMemo;
    private String rejectReason;
}
