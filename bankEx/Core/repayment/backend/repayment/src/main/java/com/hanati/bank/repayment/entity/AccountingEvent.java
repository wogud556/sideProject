package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.AccountingEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회계 시스템이 소비할 이벤트 Outbox (명세 11번).
 *
 * <p>복식부기 분개는 이번 범위에서 구현하지 않는다. 이벤트 적재는 상환 거래 저장과 동일한
 * 트랜잭션에서 수행하므로, 거래가 롤백되면 이벤트도 남지 않는다.
 */
@Entity
@Table(name = "ACCOUNTING_EVENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 40)
    private AccountingEventType eventType;

    @Column(name = "LOAN_ACCOUNT_ID", nullable = false)
    private Long loanAccountId;

    @Column(name = "REPAYMENT_TRANSACTION_ID", nullable = false)
    private Long repaymentTransactionId;

    /** 취소 이벤트는 음수 금액으로 적재한다. */
    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "PUBLISHED", nullable = false)
    private boolean published;
}
