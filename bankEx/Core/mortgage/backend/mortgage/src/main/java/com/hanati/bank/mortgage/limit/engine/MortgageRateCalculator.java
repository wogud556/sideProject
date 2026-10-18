package com.hanati.bank.mortgage.limit.engine;

import java.math.BigDecimal;

/**
 * 적용 금리 = 상품 기준금리 + 신용점수 구간별 가산금리.
 * 가격 정책이지 규제가 아니므로 정책 테이블이 아닌 코드에 둔다.
 */
public final class MortgageRateCalculator {

    private MortgageRateCalculator() {
    }

    public static BigDecimal rateFor(BigDecimal baseRate, int creditScore) {
        return baseRate.add(spreadFor(creditScore));
    }

    static BigDecimal spreadFor(int creditScore) {
        if (creditScore >= 900) return new BigDecimal("0.00");
        if (creditScore >= 800) return new BigDecimal("0.30");
        if (creditScore >= 700) return new BigDecimal("0.70");
        if (creditScore >= 600) return new BigDecimal("1.20");
        return new BigDecimal("2.00");
    }
}
