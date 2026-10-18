package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 주택 소재지의 규제지역 구분. 정책 테이블의 REGION_CODE와 같은 값을 쓴다 */
@Getter
@RequiredArgsConstructor
public enum RegionCode {
    REGULATED("규제지역"),
    NON_REGULATED("비규제지역");

    private final String label;
}
