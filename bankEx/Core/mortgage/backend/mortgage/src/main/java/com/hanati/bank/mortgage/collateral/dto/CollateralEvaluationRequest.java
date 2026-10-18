package com.hanati.bank.mortgage.collateral.dto;

import com.hanati.bank.mortgage.collateral.enums.LienType;
import com.hanati.bank.mortgage.collateral.enums.ValuationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * 담보 평가 등록. 평가액과 등기부·임대차 확인 결과(선순위 권리)를 함께 받는다.
 * 선순위 목록은 매번 통째로 교체한다.
 */
public record CollateralEvaluationRequest(
        @NotNull ValuationMethod valuationMethod,
        @NotNull @Positive Long appraisedValue,
        @NotNull @Valid List<SeniorClaim> seniorClaims
) {
    public record SeniorClaim(
            @NotNull LienType lienType,
            @NotBlank String creditor,
            @NotNull @PositiveOrZero Long amount,
            @NotNull @Positive Integer lienRank
    ) {
        @AssertTrue
        public boolean isNotOwnMortgage() {
            return lienType != LienType.OWN_MORTGAGE;
        }
    }
}
