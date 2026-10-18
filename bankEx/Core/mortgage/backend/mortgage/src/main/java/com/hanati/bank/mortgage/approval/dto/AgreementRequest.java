package com.hanati.bank.mortgage.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 고객 채널(bankEx)이 보낸다. 계좌가 고객 본인 것인지는 bankEx가 확인한 뒤 넘긴다. */
public record AgreementRequest(
        @NotBlank String customerId,
        @NotBlank String disbursementAccountNo,
        @NotNull Boolean consent
) {
}
