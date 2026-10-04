package com.hanati.bank.repayment.policy;

import com.hanati.bank.repayment.enums.AllocationType;

import java.math.BigDecimal;

/** 배분 한 줄. scheduleId가 null이면 계좌 단위 항목(비용, 중도상환 원금, 과오납)이다. */
public record AllocationLine(Long scheduleId, AllocationType type, BigDecimal amount, int order) {
}
