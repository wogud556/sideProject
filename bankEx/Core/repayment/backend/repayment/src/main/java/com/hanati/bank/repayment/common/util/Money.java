package com.hanati.bank.repayment.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원화 금액 계산 규칙. 모든 금액은 scale 0, 반올림은 HALF_UP으로 통일한다 (명세 9번).
 * double / float은 사용하지 않는다.
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(0);

    private Money() {
    }

    public static BigDecimal of(long amount) {
        return BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(0, RoundingMode.HALF_UP);
    }

    /** 두 값 중 작은 쪽. 음수가 넘어오면 0으로 막는다. */
    public static BigDecimal minNonNegative(BigDecimal a, BigDecimal b) {
        BigDecimal min = a.min(b);
        return min.signum() < 0 ? ZERO : min;
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isZero(BigDecimal amount) {
        return amount != null && amount.signum() == 0;
    }

    /** 금액 × 비율(%) → 원 단위 반올림. */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
        return amount.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
}
