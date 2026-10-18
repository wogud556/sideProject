package com.hanati.bank.mortgage.screening;

import com.hanati.bank.mortgage.screening.engine.MortgageScreeningEngine;
import com.hanati.bank.mortgage.screening.engine.ScreeningInput;
import com.hanati.bank.mortgage.screening.engine.ScreeningOutcome;
import com.hanati.bank.mortgage.screening.enums.AreaResult;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MortgageScreeningEngineTest {

    private final MortgageScreeningEngine engine = new MortgageScreeningEngine();

    private ScreeningOutcome screen(int credit, boolean income, boolean ownership, long collateralLimit,
                                    long finalLimit, long requested) {
        return engine.screen(new ScreeningInput(credit, income, ownership, collateralLimit, finalLimit, requested));
    }

    @Test
    @DisplayName("모든 영역 통과 → 승인, 권고 금액 = 신청 금액")
    void approved() {
        ScreeningOutcome o = screen(800, true, true, 350_000_000L, 350_000_000L, 300_000_000L);

        assertThat(o.status()).isEqualTo(ScreeningStatus.APPROVED);
        assertThat(o.reasons()).isEmpty();
        assertThat(o.recommendedAmount()).isEqualTo(300_000_000L);
        assertThat(o.requiresPreExecutionCheck()).isFalse();
    }

    @Test
    @DisplayName("신용점수 600 미만 → 거절 (경계값 600은 통과)")
    void lowCredit() {
        ScreeningOutcome rejected = screen(599, true, true, 350_000_000L, 350_000_000L, 300_000_000L);
        assertThat(rejected.status()).isEqualTo(ScreeningStatus.REJECTED);
        assertThat(rejected.borrowerResult()).isEqualTo(AreaResult.FAIL);
        assertThat(rejected.recommendedAmount()).isNull();

        assertThat(screen(600, true, true, 350_000_000L, 350_000_000L, 300_000_000L).status())
                .isEqualTo(ScreeningStatus.APPROVED);
    }

    @Test
    @DisplayName("최종 한도 0 → 거절 (다른 HOLD가 있어도 FAIL이 우선)")
    void noLimit() {
        ScreeningOutcome o = screen(800, false, false, 350_000_000L, 0L, 300_000_000L);

        assertThat(o.status()).isEqualTo(ScreeningStatus.REJECTED);
        assertThat(o.reasons()).contains(ScreeningReason.LIMIT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("담보가용액 0 → 거절")
    void noCollateral() {
        ScreeningOutcome o = screen(800, true, true, 0L, 0L, 300_000_000L);

        assertThat(o.collateralResult()).isEqualTo(AreaResult.FAIL);
        assertThat(o.status()).isEqualTo(ScreeningStatus.REJECTED);
    }

    @Test
    @DisplayName("소득 증빙 미확인 → 추가 서류 요청 (조건부 승인보다 우선)")
    void incomeDocumentMissing() {
        ScreeningOutcome o = screen(800, false, false, 350_000_000L, 350_000_000L, 400_000_000L);

        assertThat(o.status()).isEqualTo(ScreeningStatus.ADDITIONAL_DOCUMENT_REQUIRED);
        assertThat(o.recommendedAmount()).isNull();
    }

    @Test
    @DisplayName("소유권 서류 미확인 → 조건부 승인, 실행 전 확인 필요")
    void ownershipDocumentMissing() {
        ScreeningOutcome o = screen(800, true, false, 350_000_000L, 350_000_000L, 300_000_000L);

        assertThat(o.status()).isEqualTo(ScreeningStatus.CONDITIONALLY_APPROVED);
        assertThat(o.requiresPreExecutionCheck()).isTrue();
        assertThat(o.recommendedAmount()).isEqualTo(300_000_000L);
    }

    @Test
    @DisplayName("신청 금액 > 한도 → 한도 내 감액 조건부 승인 (실행 전 확인은 필요 없음)")
    void requestedExceedsLimit() {
        ScreeningOutcome o = screen(800, true, true, 350_000_000L, 350_000_000L, 400_000_000L);

        assertThat(o.status()).isEqualTo(ScreeningStatus.CONDITIONALLY_APPROVED);
        assertThat(o.regulatoryResult()).isEqualTo(AreaResult.HOLD);
        assertThat(o.recommendedAmount()).isEqualTo(350_000_000L);
        assertThat(o.requiresPreExecutionCheck()).isFalse();
    }
}
