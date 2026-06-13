package com.hanati.bank.screening.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScreeningResult {
    private int cssScore;
    private double dsrRate;
    private long approvedAmount;
    private String resultStatus;
    private String rejectReason;
}
