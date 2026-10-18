package com.hanati.bank.mortgage.collateral.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValuationMethod {
    MARKET_PRICE("시세 조회 (KB시세 등)"),
    APPRAISAL("감정평가");

    private final String label;
}
