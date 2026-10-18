package com.hanati.bank.mortgage.gateway;

/**
 * 대출 실행금 입금. 실제 고객 수신계좌는 bankEx에 있으므로 이 인터페이스 뒤에 숨긴다.
 * 구현체는 같은 idempotencyKey로 여러 번 호출돼도 한 번만 입금해야 한다.
 */
public interface DisbursementGateway {

    /** @throws DisbursementFailedException 입금하지 못한 경우 */
    String disburse(DisbursementCommand command);

    record DisbursementCommand(String idempotencyKey, String accountNo, long amount, String description) {
    }
}
