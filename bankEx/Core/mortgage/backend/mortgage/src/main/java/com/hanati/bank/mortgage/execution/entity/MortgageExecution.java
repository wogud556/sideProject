package com.hanati.bank.mortgage.execution.entity;

import com.hanati.bank.mortgage.execution.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 대출 실행 이력. 실패한 시도도 남겨 감사·대사에 쓴다. */
@Entity
@Table(name = "MORTGAGE_EXECUTION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortgageExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXECUTION_ID")
    private Long executionId;

    @Column(name = "EXECUTION_NO", unique = true, length = 30)
    private String executionNo;

    @Column(name = "APPLICATION_ID", nullable = false)
    private Long applicationId;

    /** 담당자 화면이 실행 버튼마다 만드는 요청 키. 같은 키의 재요청은 이 행을 그대로 돌려준다 */
    @Column(name = "IDEMPOTENCY_KEY", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "DISBURSEMENT_ACCOUNT_NO", nullable = false, length = 20)
    private String disbursementAccountNo;

    /** bankEx 입금 거래번호 (성공 시) */
    @Column(name = "BANK_TRANSACTION_NO", length = 50)
    private String bankTransactionNo;

    @Column(name = "FAILURE_REASON", length = 300)
    private String failureReason;

    @Column(name = "LOAN_ACCOUNT_ID")
    private Long loanAccountId;

    @Column(name = "EXECUTED_BY", nullable = false, length = 20)
    private String executedBy;

    @Column(name = "EXECUTED_AT", nullable = false)
    private LocalDateTime executedAt;

    public void assignExecutionNo(String executionNo) {
        this.executionNo = executionNo;
    }
}
