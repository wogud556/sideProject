package com.hanati.bank.refinance.refinance.entity;

import com.hanati.bank.refinance.refinance.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 금융거래 이력. requestId는 멱등키로 UNIQUE 제약을 건다 — 동일 requestId로 재요청해도
 * 실제 외부 금융거래(신규대출실행/기존대출상환)는 한 번만 발생해야 한다는 명세 18번 요구사항의 DB 레벨 보증.
 */
@Entity
@Table(name = "TB_REFINANCE_TRANSACTION", uniqueConstraints = @UniqueConstraint(columnNames = "REQUEST_ID"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefinanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "REQUEST_ID", unique = true, nullable = false)
    private String requestId;

    private String transactionNo;

    @Lob
    private String requestData;

    @Lob
    private String responseData;

    private String resultCode;
    private String resultMessage;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String processedBy;
}
