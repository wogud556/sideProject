package com.hanati.bank.refinance.gateway;

import com.hanati.bank.refinance.gateway.dto.LoanExecutionRequest;
import com.hanati.bank.refinance.gateway.dto.LoanExecutionResult;

/**
 * 외부 Core Banking 시스템의 신규대출 실행 연계 인터페이스 (명세 15번).
 * 실 연계가 없는 개발 환경에서는 {@link MockLoanExecutionGateway}를 사용하고,
 * 향후 실제 시스템 연동 시 CoreBankingLoanExecutionGateway 등으로 교체한다.
 */
public interface LoanExecutionGateway {
    LoanExecutionResult executeLoan(LoanExecutionRequest request);
}
