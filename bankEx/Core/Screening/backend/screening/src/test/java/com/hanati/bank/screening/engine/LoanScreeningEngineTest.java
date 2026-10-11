package com.hanati.bank.screening.engine;

import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.enums.LoanType;
import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanScreeningEngineTest {

    private final LoanScreeningEngine engine = new LoanScreeningEngine();

    private static ScreeningRequest request(long requested, long income, int creditScore, long existing) {
        return new ScreeningRequest("C1", "1", LoanType.GENERAL, requested, income, creditScore, existing);
    }

    @Test
    @DisplayName("기본 조건 충족 시 신청 금액 전액과 신용점수 구간 금리로 승인한다")
    void approve() {
        ScreeningResult r = engine.screen(request(30_000_000L, 60_000_000L, 850, 10_000_000L));

        assertThat(r.getStatus()).isEqualTo(ScreeningStatus.APPROVED);
        assertThat(r.getApprovedAmount()).isEqualTo(30_000_000L);
        assertThat(r.getApprovedInterestRate()).isEqualByComparingTo("4.20");
        assertThat(r.getReasonCode()).isEqualTo(ScreeningReasonCode.BASIC_CRITERIA_MET);
    }

    @Test
    @DisplayName("신용점수 599점은 거절, 600점은 거절하지 않는다")
    void creditScoreRejectBoundary() {
        assertThat(engine.screen(request(10_000_000L, 50_000_000L, 599, 0L)).getReasonCode())
                .isEqualTo(ScreeningReasonCode.LOW_CREDIT_SCORE);
        assertThat(engine.screen(request(10_000_000L, 50_000_000L, 600, 0L)).getStatus())
                .isNotEqualTo(ScreeningStatus.REJECTED);
    }

    @Test
    @DisplayName("신용점수 600~699점은 수동심사, 700점부터 자동 승인한다")
    void creditScoreManualReviewBoundary() {
        ScreeningResult borderline = engine.screen(request(10_000_000L, 50_000_000L, 699, 0L));
        assertThat(borderline.getStatus()).isEqualTo(ScreeningStatus.MANUAL_REVIEW);
        assertThat(borderline.getReasonCode()).isEqualTo(ScreeningReasonCode.BORDERLINE_CREDIT_SCORE);
        assertThat(borderline.getApprovedAmount()).isNull();

        assertThat(engine.screen(request(10_000_000L, 50_000_000L, 700, 0L)).getStatus())
                .isEqualTo(ScreeningStatus.APPROVED);
    }

    @Test
    @DisplayName("기존대출 + 신청액이 연소득의 10배와 같으면 통과, 1원이라도 넘으면 거절한다")
    void totalDebtBoundary() {
        // 연소득 5천만 → 한도 5억. 신청액은 500% 이하로 맞춘다
        assertThat(engine.screen(request(100_000_000L, 50_000_000L, 800, 400_000_000L)).getStatus())
                .isEqualTo(ScreeningStatus.APPROVED);
        assertThat(engine.screen(request(100_000_000L, 50_000_000L, 800, 400_000_001L)).getReasonCode())
                .isEqualTo(ScreeningReasonCode.DEBT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("신청액이 연소득의 500%와 같으면 승인, 넘으면 수동심사로 보낸다")
    void requestIncomeMultipleBoundary() {
        assertThat(engine.screen(request(250_000_000L, 50_000_000L, 800, 0L)).getStatus())
                .isEqualTo(ScreeningStatus.APPROVED);

        ScreeningResult over = engine.screen(request(250_000_001L, 50_000_000L, 800, 0L));
        assertThat(over.getStatus()).isEqualTo(ScreeningStatus.MANUAL_REVIEW);
        assertThat(over.getReasonCode()).isEqualTo(ScreeningReasonCode.INCOME_MULTIPLE_EXCEEDED);
    }

    @Test
    @DisplayName("연소득 0원이면 부채 한도 초과로 거절한다 (0으로 나누지 않는다)")
    void zeroIncome() {
        assertThat(engine.screen(request(1_000_000L, 0L, 800, 0L)).getReasonCode())
                .isEqualTo(ScreeningReasonCode.DEBT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("거절 사유가 수동심사 사유보다 우선한다")
    void rejectTakesPriority() {
        // 신용점수 미달 + 소득 배수 초과 → 신용점수 거절
        assertThat(engine.screen(request(300_000_000L, 50_000_000L, 550, 0L)).getReasonCode())
                .isEqualTo(ScreeningReasonCode.LOW_CREDIT_SCORE);
        // 부채 한도 초과 + 600점대 → 부채 거절
        assertThat(engine.screen(request(100_000_000L, 10_000_000L, 650, 50_000_000L)).getReasonCode())
                .isEqualTo(ScreeningReasonCode.DEBT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("신용점수 구간별 금리")
    void interestRateTiers() {
        assertThat(engine.interestRateFor(900)).isEqualByComparingTo(new BigDecimal("3.50"));
        assertThat(engine.interestRateFor(899)).isEqualByComparingTo(new BigDecimal("4.20"));
        assertThat(engine.interestRateFor(800)).isEqualByComparingTo(new BigDecimal("4.20"));
        assertThat(engine.interestRateFor(700)).isEqualByComparingTo(new BigDecimal("5.50"));
        assertThat(engine.interestRateFor(699)).isEqualByComparingTo(new BigDecimal("7.00"));
    }
}
