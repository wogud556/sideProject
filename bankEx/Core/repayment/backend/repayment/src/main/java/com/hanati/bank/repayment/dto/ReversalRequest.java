package com.hanati.bank.repayment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 명세 8.4 요청. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReversalRequest {

    @NotBlank
    private String reason;

    @NotBlank
    private String idempotencyKey;

    private String createdBy;
}
