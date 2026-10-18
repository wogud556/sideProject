package com.hanati.bank.mortgage.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** bankEx 없이 Core만 띄우거나 테스트할 때 쓴다 (mortgage.gateway.mode=mock). 항상 성공한다. */
@Component
@ConditionalOnProperty(name = "mortgage.gateway.mode", havingValue = "mock")
public class MockDisbursementGateway implements DisbursementGateway {

    @Override
    public String disburse(DisbursementCommand command) {
        return "MOCK-" + command.idempotencyKey();
    }
}
