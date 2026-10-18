package com.hanati.bank.mortgage.repayment.calculator;

import com.hanati.bank.mortgage.application.enums.RepaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상환 예정표 계산 (명세 7). 월 이자 = 잔액 × 연이율 / 12, 원 단위 반올림.
 * 원금 끝전은 마지막 회차가 흡수해서 회차별 원금의 합이 대출 원금과 정확히 같다.
 */
public interface RepaymentCalculator {

    List<ScheduleLine> calculate(long principal, BigDecimal annualRatePercent, int months);

    static RepaymentCalculator of(RepaymentMethod method) {
        return switch (method) {
            case EQUAL_INSTALLMENT -> new EqualInstallmentCalculator();
            case EQUAL_PRINCIPAL -> new EqualPrincipalCalculator();
            case BULLET -> new BulletRepaymentCalculator();
        };
    }
}
