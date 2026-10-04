package com.hanati.bank.refinance.gateway.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 신규대출 실행 요청. refinanceTargetAccountNos는 이 신규대출이 어떤 기존대출 상환 목적으로
 * 실행되는지 코어뱅킹 측에 함께 통보하는 컨텍스트 정보(대환 목적 대출의 일반적 관행)이며,
 * Mock 구현체는 이를 이용해 데모 실패 시나리오를 결정적으로 재현한다.
 */
public record LoanExecutionRequest(
        String requestId,
        Long applicationId,
        Long customerId,
        String productName,
        BigDecimal amount,
        String depositAccountNo,
        List<String> refinanceTargetAccountNos
) {
}
