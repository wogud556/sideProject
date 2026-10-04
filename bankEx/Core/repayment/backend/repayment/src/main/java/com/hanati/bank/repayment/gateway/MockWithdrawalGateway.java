package com.hanati.bank.repayment.gateway;

import com.hanati.bank.repayment.gateway.dto.RefundRequest;
import com.hanati.bank.repayment.gateway.dto.RefundResult;
import com.hanati.bank.repayment.gateway.dto.WithdrawalRequest;
import com.hanati.bank.repayment.gateway.dto.WithdrawalResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 개발 환경용 Mock 출금 게이트웨이. {@link DemoScenarioAccounts}의 계좌는 항상 실패를 반환해
 * 출금 실패 경로를 결정적으로 재현할 수 있게 한다.
 */
@Component
public class MockWithdrawalGateway implements WithdrawalGateway {

    @Override
    public WithdrawalResult withdraw(WithdrawalRequest request) {
        if (DemoScenarioAccounts.WITHDRAWAL_FAILURE_ACCOUNT.equals(request.accountNumber())) {
            return new WithdrawalResult(false, null, request.amount(),
                    "INSUFFICIENT_BALANCE", "출금계좌 잔액이 부족합니다 (데모 시나리오)");
        }
        return new WithdrawalResult(true, newTransactionNo("WD"), request.amount(), "0000", "출금 완료");
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        if (DemoScenarioAccounts.REFUND_FAILURE_ACCOUNT.equals(request.accountNumber())) {
            return new RefundResult(false, null, "REFUND_REJECTED", "환급 입금이 거절되었습니다 (데모 시나리오)");
        }
        return new RefundResult(true, newTransactionNo("RF"), "0000", "환급 완료");
    }

    private String newTransactionNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
