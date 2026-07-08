package com.hanati.bank.bankEx.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class JeonseLoanExecuteResponse {
    private String applicationId;
    private String status;
    private Long executedAmount;
    private LocalDate firstPaymentDate;
    private String message;
}
