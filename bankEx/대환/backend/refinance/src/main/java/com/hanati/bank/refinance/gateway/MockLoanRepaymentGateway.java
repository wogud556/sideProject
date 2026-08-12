package com.hanati.bank.refinance.gateway;

import com.hanati.bank.refinance.gateway.dto.RepaymentInquiryRequest;
import com.hanati.bank.refinance.gateway.dto.RepaymentInquiryResult;
import com.hanati.bank.refinance.gateway.dto.RepaymentRequest;
import com.hanati.bank.refinance.gateway.dto.RepaymentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개발 환경용 Mock 기존대출 상환 게이트웨이. {@link DemoScenarioAccounts#REPAYMENT_FAILURE_ACCOUNT}는
 * 최초 1회 호출만 실패하고 이후(재처리) 호출부터는 성공하도록 계좌별 시도 횟수를 기억한다 —
 * "신규대출 성공 + 기존대출 상환 실패 → 재처리 → 성공" 시나리오(명세 33/34번)를 결정적으로 재현하기 위함.
 */
@Component
public class MockLoanRepaymentGateway implements LoanRepaymentGateway {

    private final Map<String, Integer> attemptCounts = new ConcurrentHashMap<>();

    @Override
    public RepaymentInquiryResult inquireRepaymentAmount(RepaymentInquiryRequest request) {
        return new RepaymentInquiryResult(null, "0000", "상환금액 재조회는 내부 계산값을 사용합니다.");
    }

    @Override
    public RepaymentResult repay(RepaymentRequest request) {
        int attempt = attemptCounts.merge(request.loanAccountNo(), 1, Integer::sum);

        if (DemoScenarioAccounts.REPAYMENT_FAILURE_ACCOUNT.equals(request.loanAccountNo()) && attempt == 1) {
            return new RepaymentResult(false, null, null, "REPAY_TIMEOUT", "상환 처리 중 타행 응답 지연 (데모 시나리오, 재처리 시 성공)");
        }

        String transactionNo = "REP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new RepaymentResult(true, transactionNo, request.amount(), "0000", "기존대출 상환 완료");
    }
}
