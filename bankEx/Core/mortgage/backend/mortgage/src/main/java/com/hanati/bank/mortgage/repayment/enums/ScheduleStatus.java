package com.hanati.bank.mortgage.repayment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleStatus {
    SCHEDULED("예정"),
    PAID("납부 완료");

    private final String label;
}
