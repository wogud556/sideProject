package com.hanati.bank.repayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 명세 8.2 요청. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentRequest {

    @NotNull
    private String repaymentType;

    @NotNull
    private String paymentMethod;

    /** 자동이체(AUTO_DEBIT)인 경우 생략하면 도래분 전액을 출금한다. */
    private BigDecimal amount;

    @NotNull
    private LocalDate paymentDate;

    @NotBlank
    private String idempotencyKey;

    private String createdBy;
}
