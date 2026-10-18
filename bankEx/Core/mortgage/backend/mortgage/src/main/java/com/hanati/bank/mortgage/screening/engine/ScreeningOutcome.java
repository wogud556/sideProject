package com.hanati.bank.mortgage.screening.engine;

import com.hanati.bank.mortgage.screening.enums.AreaResult;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;

import java.util.List;

/**
 * @param recommendedAmount        승인 권고 금액. 승인·조건부 승인일 때만 값이 있다
 * @param requiresPreExecutionCheck 실행 전에 담당자가 조건 이행을 확인해야 하는지 여부
 */
public record ScreeningOutcome(
        ScreeningStatus status,
        AreaResult borrowerResult,
        AreaResult collateralResult,
        AreaResult regulatoryResult,
        List<ScreeningReason> reasons,
        Long recommendedAmount,
        boolean requiresPreExecutionCheck
) {
}
