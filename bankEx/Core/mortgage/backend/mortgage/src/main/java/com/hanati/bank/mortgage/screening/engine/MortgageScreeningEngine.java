package com.hanati.bank.mortgage.screening.engine;

import com.hanati.bank.mortgage.screening.enums.AreaResult;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 여신 심사 엔진 (명세 6). 한도 산정 결과는 입력값 중 하나일 뿐이고, 승인 여부는 여기서 정한다.
 *
 * 영역별 판정
 *   차주: 신용점수 < 600 → FAIL / 소득 증빙 미확인 → HOLD
 *   담보: 담보가용액 0 → FAIL / 소유권 서류 미확인 → HOLD
 *   규제: 최종 한도 0 → FAIL / 신청 금액 > 최종 한도 → HOLD (한도 내 감액)
 *
 * 종합 판정 (위에서부터)
 *   FAIL이 하나라도 있으면 → 거절
 *   소득 증빙 미확인 → 추가 서류 요청 (상환능력을 판단할 수 없으므로 조건부 승인도 하지 않는다)
 *   HOLD가 있으면 → 조건부 승인
 *   그 외 → 승인
 */
@Component
public class MortgageScreeningEngine {

    private static final int MIN_CREDIT_SCORE = 600;

    public ScreeningOutcome screen(ScreeningInput in) {
        List<ScreeningReason> reasons = new ArrayList<>();

        AreaResult borrower = AreaResult.PASS;
        if (in.creditScore() < MIN_CREDIT_SCORE) {
            borrower = AreaResult.FAIL;
            reasons.add(ScreeningReason.LOW_CREDIT_SCORE);
        } else if (!in.incomeVerified()) {
            borrower = AreaResult.HOLD;
            reasons.add(ScreeningReason.INCOME_DOCUMENT_MISSING);
        }

        AreaResult collateral = AreaResult.PASS;
        if (in.collateralLimit() <= 0) {
            collateral = AreaResult.FAIL;
            reasons.add(ScreeningReason.NO_COLLATERAL_CAPACITY);
        } else if (!in.ownershipVerified()) {
            collateral = AreaResult.HOLD;
            reasons.add(ScreeningReason.OWNERSHIP_DOCUMENT_MISSING);
        }

        AreaResult regulatory = AreaResult.PASS;
        if (in.finalLimit() <= 0) {
            regulatory = AreaResult.FAIL;
            reasons.add(ScreeningReason.LIMIT_NOT_AVAILABLE);
        } else if (in.requestedAmount() > in.finalLimit()) {
            regulatory = AreaResult.HOLD;
            reasons.add(ScreeningReason.REQUESTED_EXCEEDS_LIMIT);
        }

        ScreeningStatus status;
        if (List.of(borrower, collateral, regulatory).contains(AreaResult.FAIL)) {
            status = ScreeningStatus.REJECTED;
        } else if (borrower == AreaResult.HOLD) {
            status = ScreeningStatus.ADDITIONAL_DOCUMENT_REQUIRED;
        } else if (collateral == AreaResult.HOLD || regulatory == AreaResult.HOLD) {
            status = ScreeningStatus.CONDITIONALLY_APPROVED;
        } else {
            status = ScreeningStatus.APPROVED;
        }

        boolean approvable = status == ScreeningStatus.APPROVED || status == ScreeningStatus.CONDITIONALLY_APPROVED;
        return new ScreeningOutcome(
                status, borrower, collateral, regulatory, List.copyOf(reasons),
                approvable ? Math.min(in.requestedAmount(), in.finalLimit()) : null,
                approvable && collateral == AreaResult.HOLD);
    }
}
