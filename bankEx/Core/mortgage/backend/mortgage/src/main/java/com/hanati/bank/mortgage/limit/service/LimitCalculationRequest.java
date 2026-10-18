package com.hanati.bank.mortgage.limit.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 심사역이 신용조회 결과로 확인한 신용점수를 넣는다. 금리 산정에 쓰인다. */
public record LimitCalculationRequest(
        @NotNull @Min(0) @Max(1000) Integer creditScore
) {
}
