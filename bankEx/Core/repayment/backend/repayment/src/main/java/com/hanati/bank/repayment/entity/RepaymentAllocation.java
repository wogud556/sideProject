package com.hanati.bank.repayment.entity;

import com.hanati.bank.repayment.enums.AllocationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상환금이 어떤 채무 항목에 얼마 배분되었는지의 원장 (명세 6.4).
 *
 * <p>완료된 배분은 수정하지 않는다. 취소는 allocatedAmount가 음수인 역배분 행을 추가해 표현한다.
 */
@Entity
@Table(name = "REPAYMENT_ALLOCATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REPAYMENT_TRANSACTION_ID", nullable = false)
    private Long repaymentTransactionId;

    /** 계좌 단위 항목(비용, 중도상환 원금, 과오납)은 null. */
    @Column(name = "REPAYMENT_SCHEDULE_ID")
    private Long repaymentScheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALLOCATION_TYPE", nullable = false, length = 30)
    private AllocationType allocationType;

    @Column(name = "ALLOCATED_AMOUNT", nullable = false, precision = 18, scale = 0)
    private BigDecimal allocatedAmount;

    @Column(name = "ALLOCATION_ORDER", nullable = false)
    private Integer allocationOrder;
}
