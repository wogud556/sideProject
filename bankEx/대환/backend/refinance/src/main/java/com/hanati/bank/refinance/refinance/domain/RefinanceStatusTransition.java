package com.hanati.bank.refinance.refinance.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.hanati.bank.refinance.refinance.domain.RefinanceStatus.*;

/**
 * 대환 신청 상태의 허용 전이만 화이트리스트로 관리한다.
 * 재처리(FAILED -> EXECUTING / FAILED -> REPAYING)는 신규대출 실행 여부(TB_REFINANCE_ERROR.failedStep)에 따라
 * RefinanceRetryService가 둘 중 하나로만 분기하며, 신규대출이 이미 실행된 이후에는
 * 절대 EXECUTING(신규대출 실행 단계)으로 되돌아가지 않는다 — 신규대출 중복 실행 방지의 핵심 불변조건.
 */
public class RefinanceStatusTransition {

    private static final Map<RefinanceStatus, Set<RefinanceStatus>> ALLOWED = new EnumMap<>(RefinanceStatus.class);

    static {
        ALLOWED.put(DRAFT, EnumSet.of(REQUESTED, CANCELLED));
        ALLOWED.put(REQUESTED, EnumSet.of(REVIEWING, CANCELLED));
        ALLOWED.put(REVIEWING, EnumSet.of(APPROVED, REJECTED, CANCELLED));
        ALLOWED.put(APPROVED, EnumSet.of(EXECUTING, CANCELLED));
        ALLOWED.put(REJECTED, EnumSet.noneOf(RefinanceStatus.class));
        ALLOWED.put(EXECUTING, EnumSet.of(NEW_LOAN_EXECUTED, FAILED));
        ALLOWED.put(NEW_LOAN_EXECUTED, EnumSet.of(REPAYING));
        ALLOWED.put(REPAYING, EnumSet.of(COMPLETED, FAILED));
        ALLOWED.put(FAILED, EnumSet.of(EXECUTING, REPAYING));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(RefinanceStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(RefinanceStatus.class));
    }

    private RefinanceStatusTransition() {
    }

    public static boolean isAllowed(RefinanceStatus from, RefinanceStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
