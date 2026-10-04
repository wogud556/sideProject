package com.hanati.bank.refinance.gateway;

import com.hanati.bank.refinance.gateway.dto.LoanExecutionRequest;
import com.hanati.bank.refinance.gateway.dto.LoanExecutionResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 개발 환경용 Mock 신규대출 실행 게이트웨이. {@link DemoScenarioAccounts#EXECUTION_FAILURE_ACCOUNT}가
 * 대환 대상에 포함된 경우 결정적으로 거절 응답을 반환해, "신규대출 실행 실패" 테스트 시나리오(명세 33번)를 재현한다.
 */
@Component
public class MockLoanExecutionGateway implements LoanExecutionGateway {

    @Override
    public LoanExecutionResult executeLoan(LoanExecutionRequest request) {
        if (request.refinanceTargetAccountNos().contains(DemoScenarioAccounts.EXECUTION_FAILURE_ACCOUNT)) {
            return new LoanExecutionResult(false, null, null, "EXEC_DECLINED", "코어뱅킹 심사 거절 (데모 시나리오)");
        }

        String transactionNo = "NLN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new LoanExecutionResult(true, transactionNo, request.amount(), "0000", "신규대출 실행 완료");
    }
}
