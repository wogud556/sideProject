package com.hanati.bank.screening.service;

import com.hanati.bank.screening.dto.ReviewDecisionRequest;
import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.dto.ScreeningResponse;
import com.hanati.bank.screening.enums.LoanType;
import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import com.hanati.bank.screening.exception.BusinessException;
import com.hanati.bank.screening.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoanScreeningServiceTest {

    @Autowired
    LoanScreeningService service;

    /** 신용점수 650점 → 수동심사 */
    private ScreeningResponse manualReview() {
        return service.screen(new ScreeningRequest("C-TEST", "1", LoanType.GENERAL,
                30_000_000L, 50_000_000L, 650, 0L));
    }

    private ScreeningResponse autoApproved() {
        return service.screen(new ScreeningRequest("C-TEST", "1", LoanType.GENERAL,
                30_000_000L, 50_000_000L, 850, 0L));
    }

    @Test
    @DisplayName("심사 요청을 저장하고 조회할 수 있다")
    void screenAndGet() {
        ScreeningResponse saved = autoApproved();

        ScreeningResponse found = service.getScreening(saved.getScreeningId());
        assertThat(found.getStatus()).isEqualTo(ScreeningStatus.APPROVED);
        assertThat(found.getReasonMessage()).isEqualTo(ScreeningReasonCode.BASIC_CRITERIA_MET.getMessage());
        assertThat(found.getScreenedAt()).isNotNull();
    }

    @Test
    @DisplayName("상태로 필터링하면 해당 상태 건만 돌려준다")
    void filterByStatus() {
        Long manualId = manualReview().getScreeningId();
        Long approvedId = autoApproved().getScreeningId();

        assertThat(service.getScreenings(ScreeningStatus.MANUAL_REVIEW))
                .extracting(ScreeningResponse::getScreeningId)
                .contains(manualId).doesNotContain(approvedId);
    }

    @Test
    @DisplayName("심사역이 감액 승인하면 승인 금액·금리·심사역이 기록된다")
    void approveWithReducedAmount() {
        Long id = manualReview().getScreeningId();

        ScreeningResponse r = service.approve(id, new ReviewDecisionRequest("REVIEWER01", 20_000_000L, "소득 증빙 확인"));

        assertThat(r.getStatus()).isEqualTo(ScreeningStatus.APPROVED);
        assertThat(r.getApprovedAmount()).isEqualTo(20_000_000L);
        assertThat(r.getApprovedInterestRate()).isEqualByComparingTo("7.00");
        assertThat(r.getReasonCode()).isEqualTo(ScreeningReasonCode.REVIEWER_APPROVED);
        assertThat(r.getReviewerId()).isEqualTo("REVIEWER01");
        assertThat(r.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("승인 금액을 비우면 신청 금액 전액을 승인한다")
    void approveFullAmountByDefault() {
        Long id = manualReview().getScreeningId();

        assertThat(service.approve(id, new ReviewDecisionRequest("REVIEWER01", null, null)).getApprovedAmount())
                .isEqualTo(30_000_000L);
    }

    @Test
    @DisplayName("승인 금액이 0원 이하이거나 신청 금액을 넘으면 거부한다")
    void rejectInvalidApprovedAmount() {
        Long id = manualReview().getScreeningId();

        assertThatThrownBy(() -> service.approve(id, new ReviewDecisionRequest("REVIEWER01", 30_000_001L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPROVED_AMOUNT);
        assertThatThrownBy(() -> service.approve(id, new ReviewDecisionRequest("REVIEWER01", 0L, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPROVED_AMOUNT);
    }

    @Test
    @DisplayName("심사역이 거절하면 승인 금액 없이 거절 상태가 된다")
    void reject() {
        Long id = manualReview().getScreeningId();

        ScreeningResponse r = service.reject(id, new ReviewDecisionRequest("REVIEWER01", null, "재직 확인 불가"));

        assertThat(r.getStatus()).isEqualTo(ScreeningStatus.REJECTED);
        assertThat(r.getApprovedAmount()).isNull();
        assertThat(r.getReasonCode()).isEqualTo(ScreeningReasonCode.REVIEWER_REJECTED);
        assertThat(r.getReviewComment()).isEqualTo("재직 확인 불가");
    }

    @Test
    @DisplayName("수동심사 대기가 아닌 건은 승인·거절할 수 없다 (이중 결정 방지)")
    void decideOnlyManualReview() {
        Long autoId = autoApproved().getScreeningId();
        assertThatThrownBy(() -> service.reject(autoId, new ReviewDecisionRequest("REVIEWER01", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.SCREENING_NOT_MANUAL_REVIEW);

        Long manualId = manualReview().getScreeningId();
        service.approve(manualId, new ReviewDecisionRequest("REVIEWER01", null, null));
        assertThatThrownBy(() -> service.reject(manualId, new ReviewDecisionRequest("REVIEWER02", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.SCREENING_NOT_MANUAL_REVIEW);
    }

    @Test
    @DisplayName("없는 심사 건은 SCREENING_NOT_FOUND")
    void notFound() {
        assertThatThrownBy(() -> service.getScreening(999_999L))
                .extracting("errorCode").isEqualTo(ErrorCode.SCREENING_NOT_FOUND);
    }
}
