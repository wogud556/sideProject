package com.hanati.bank.mortgage.application.enums;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.hanati.bank.mortgage.application.enums.ApplicationStatus.*;

/**
 * 신청 상태 전이 규칙. 상태 규칙을 바꿀 때는 서비스가 아니라 여기를 고친다.
 *
 * 심사 승인과 대출 승인, 약정과 실행을 서로 다른 단계로 둔다.
 * 승인을 받아도 약정·담보권 설정·조건 이행을 거쳐 EXECUTION_READY가 되어야 실행할 수 있다.
 */
public final class ApplicationStatusTransition {

    /** 담보 재평가나 한도 재산정, 추가 서류 후 재심사처럼 같은 상태로 다시 들어오는 경우도 허용한다. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.ofEntries(
            Map.entry(APPLIED, EnumSet.of(COLLATERAL_EVALUATED, WITHDRAWN)),
            Map.entry(COLLATERAL_EVALUATED, EnumSet.of(COLLATERAL_EVALUATED, LIMIT_CALCULATED, WITHDRAWN)),
            Map.entry(LIMIT_CALCULATED, EnumSet.of(COLLATERAL_EVALUATED, LIMIT_CALCULATED,
                    SCREENING_APPROVED, CONDITIONALLY_APPROVED, DOCUMENT_REQUIRED, REJECTED, WITHDRAWN)),
            Map.entry(DOCUMENT_REQUIRED, EnumSet.of(COLLATERAL_EVALUATED, LIMIT_CALCULATED,
                    SCREENING_APPROVED, CONDITIONALLY_APPROVED, DOCUMENT_REQUIRED, REJECTED, WITHDRAWN)),
            Map.entry(SCREENING_APPROVED, EnumSet.of(APPROVED, WITHDRAWN)),
            Map.entry(CONDITIONALLY_APPROVED, EnumSet.of(APPROVED, WITHDRAWN)),
            Map.entry(APPROVED, EnumSet.of(AGREED, WITHDRAWN)),
            Map.entry(AGREED, EnumSet.of(EXECUTION_READY, WITHDRAWN)),
            Map.entry(EXECUTION_READY, EnumSet.of(EXECUTED, WITHDRAWN)),
            Map.entry(REJECTED, EnumSet.noneOf(ApplicationStatus.class)),
            Map.entry(EXECUTED, EnumSet.noneOf(ApplicationStatus.class)),
            Map.entry(WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class))
    );

    private ApplicationStatusTransition() {
    }

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    public static void validate(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}
