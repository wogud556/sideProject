package com.hanati.bank.mortgage.screening.engine;

/**
 * @param incomeVerified    소득 증빙(원천징수영수증 등) 확인 여부
 * @param ownershipVerified 등기부등본·소유권 서류 확인 여부
 */
public record ScreeningInput(
        int creditScore,
        boolean incomeVerified,
        boolean ownershipVerified,
        long collateralLimit,
        long finalLimit,
        long requestedAmount
) {
}
