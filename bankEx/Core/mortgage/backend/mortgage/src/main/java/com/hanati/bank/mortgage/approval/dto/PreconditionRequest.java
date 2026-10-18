package com.hanati.bank.mortgage.approval.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 실행 선행조건 확인. 호출하면 당행 근저당 설정을 등록하고,
 * conditionsFulfilled가 true면 조건부 승인의 조건 이행도 확인 처리한다.
 */
public record PreconditionRequest(
        @NotNull Boolean conditionsFulfilled
) {
}
