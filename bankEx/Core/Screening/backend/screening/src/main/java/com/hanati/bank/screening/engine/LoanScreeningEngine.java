package com.hanati.bank.screening.engine;

import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 명세 §9.4 초기 심사 규칙. 실제 신용평가·DSR이 아닌 테스트용 내부 기준이다.
 * 거절 규칙을 수동심사 규칙보다 먼저 평가한다.
 */
@Component
public class LoanScreeningEngine {

    static final int REJECT_CREDIT_SCORE = 600;
    static final int MANUAL_REVIEW_CREDIT_SCORE = 700;
    static final long TOTAL_DEBT_INCOME_MULTIPLE = 10;
    static final long REQUEST_INCOME_MULTIPLE = 5;

    public ScreeningResult screen(ScreeningRequest req) {
        int creditScore = req.getCreditScore();
        long income = req.getAnnualIncome();
        long requested = req.getRequestedAmount();

        if (creditScore < REJECT_CREDIT_SCORE) {
            return decided(ScreeningStatus.REJECTED, ScreeningReasonCode.LOW_CREDIT_SCORE);
        }
        if (req.getExistingLoanAmount() + requested > income * TOTAL_DEBT_INCOME_MULTIPLE) {
            return decided(ScreeningStatus.REJECTED, ScreeningReasonCode.DEBT_LIMIT_EXCEEDED);
        }
        if (requested > income * REQUEST_INCOME_MULTIPLE) {
            return decided(ScreeningStatus.MANUAL_REVIEW, ScreeningReasonCode.INCOME_MULTIPLE_EXCEEDED);
        }
        if (creditScore < MANUAL_REVIEW_CREDIT_SCORE) {
            return decided(ScreeningStatus.MANUAL_REVIEW, ScreeningReasonCode.BORDERLINE_CREDIT_SCORE);
        }
        return new ScreeningResult(ScreeningStatus.APPROVED, requested, interestRateFor(creditScore),
                ScreeningReasonCode.BASIC_CRITERIA_MET);
    }

    /** 신용점수 구간별 금리. 심사역 승인 시에도 같은 기준을 쓴다 */
    public BigDecimal interestRateFor(int creditScore) {
        if (creditScore >= 900) return new BigDecimal("3.50");
        if (creditScore >= 800) return new BigDecimal("4.20");
        if (creditScore >= 700) return new BigDecimal("5.50");
        return new BigDecimal("7.00");
    }

    private ScreeningResult decided(ScreeningStatus status, ScreeningReasonCode reasonCode) {
        return new ScreeningResult(status, null, null, reasonCode);
    }
}
