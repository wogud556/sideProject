package com.hanati.bank.mortgage.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 담보 주택 유형 */
@Getter
@RequiredArgsConstructor
public enum PropertyType {
    APARTMENT("아파트"),
    VILLA("연립·다세대"),
    DETACHED("단독주택"),
    OFFICETEL("주거용 오피스텔");

    private final String label;
}
