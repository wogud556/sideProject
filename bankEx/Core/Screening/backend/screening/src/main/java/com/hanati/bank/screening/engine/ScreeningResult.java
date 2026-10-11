package com.hanati.bank.screening.engine;

import com.hanati.bank.screening.enums.ScreeningReasonCode;
import com.hanati.bank.screening.enums.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ScreeningResult {
    private ScreeningStatus status;
    /** APPROVED일 때만 값이 있다 */
    private Long approvedAmount;
    private BigDecimal approvedInterestRate;
    private ScreeningReasonCode reasonCode;
}
