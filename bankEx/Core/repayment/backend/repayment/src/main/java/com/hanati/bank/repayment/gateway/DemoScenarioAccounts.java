package com.hanati.bank.repayment.gateway;

/** Mock 게이트웨이가 결정적으로 실패를 재현하는 데모 계좌번호. */
public final class DemoScenarioAccounts {

    /** 잔액 부족으로 자동이체 출금이 항상 실패하는 계좌. */
    public static final String WITHDRAWAL_FAILURE_ACCOUNT = "110-000-999999";

    /** 환급 입금이 항상 실패하는 계좌. */
    public static final String REFUND_FAILURE_ACCOUNT = "110-000-888888";

    private DemoScenarioAccounts() {
    }
}
