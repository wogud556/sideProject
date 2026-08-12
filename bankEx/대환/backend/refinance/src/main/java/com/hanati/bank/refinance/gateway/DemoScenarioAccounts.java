package com.hanati.bank.refinance.gateway;

/**
 * Mock Gateway가 결정적으로 실패/성공을 재현하기 위해 참조하는 데모 전용 대출계좌번호.
 * 실제 심사/한도 로직과 무관하며, DataInitializer가 시딩하는 시나리오 고객 데이터와 짝을 이룬다 (명세 33번).
 */
public class DemoScenarioAccounts {

    /** 신규대출 실행 자체가 실패하는 데모 계좌 (FAILED 처리 후 EXECUTING 단계부터 재처리) */
    public static final String EXECUTION_FAILURE_ACCOUNT = "110-9999-000101";

    /** 신규대출 실행은 성공하지만 기존대출 상환이 최초 1회 실패하는 데모 계좌 (재처리 시 상환만 재시도, 신규대출 재실행 없음) */
    public static final String REPAYMENT_FAILURE_ACCOUNT = "110-9999-000202";

    private DemoScenarioAccounts() {
    }
}
