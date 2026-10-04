package com.hanati.bank.repayment.gateway;

import com.hanati.bank.repayment.gateway.dto.RefundRequest;
import com.hanati.bank.repayment.gateway.dto.RefundResult;
import com.hanati.bank.repayment.gateway.dto.WithdrawalRequest;
import com.hanati.bank.repayment.gateway.dto.WithdrawalResult;

/**
 * 외부 수신계좌 시스템 연계 (명세 3.2). 자동이체 상환의 출금과 과오납 환급 입금을 담당한다.
 * 실제 계좌 시스템과 연동하지 않으므로 Mock 구현체를 함께 제공한다.
 */
public interface WithdrawalGateway {

    WithdrawalResult withdraw(WithdrawalRequest request);

    RefundResult refund(RefundRequest request);
}
