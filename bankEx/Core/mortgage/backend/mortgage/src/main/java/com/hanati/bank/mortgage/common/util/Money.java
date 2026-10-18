package com.hanati.bank.mortgage.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원화 금액 계산 규칙. 금액은 원 단위(scale 0)이고 double/float은 쓰지 않는다.
 * 이자는 HALF_UP 반올림, 한도는 고객에게 불리하지 않도록 DOWN(버림)으로 자른다.
 */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    private Money() {
    }

    /** 금액 × 비율(%) → 원 단위 버림. 한도 계산용. */
    public static long percentOfDown(long amount, BigDecimal ratePercent) {
        return BigDecimal.valueOf(amount).multiply(ratePercent)
                .divide(HUNDRED, 0, RoundingMode.DOWN).longValueExact();
    }

    /** 만원 미만 버림. 산정 한도를 만원 단위로 끊는다. */
    public static long floorToTenThousand(BigDecimal amount) {
        if (amount.signum() <= 0) {
            return 0L;
        }
        return amount.divide(TEN_THOUSAND, 0, RoundingMode.DOWN).multiply(TEN_THOUSAND).longValueExact();
    }

    public static long roundWon(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
