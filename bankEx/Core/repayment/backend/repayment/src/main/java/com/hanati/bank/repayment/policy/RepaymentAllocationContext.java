package com.hanati.bank.repayment.policy;

import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 배분 정책 입력값 (명세 4번). */
@Getter
@Builder
public class RepaymentAllocationContext {

    private final RepaymentTransactionType transactionType;
    private final LocalDate businessDate;
    private final BigDecimal paymentAmount;

    /** 계좌에 남은 비용 잔액. */
    private final BigDecimal feeBalance;

    /** 중도상환/전액상환 시에만 산출되는 중도상환수수료. 비용과 함께 FEE로 배분한다. */
    private final BigDecimal prepaymentFee;

    /** 계좌의 정상 원금잔액. 연체원금은 포함하지 않는다. */
    private final BigDecimal principalBalance;

    /** 명세 5번 기준으로 정렬된 회차 목록 (납부예정일 → 회차 → ID 오름차순). */
    private final List<ScheduleSnapshot> schedules;
}
