package com.hanati.bank.repayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 명세 8.3 요청. expectedTotalAmount는 8.1로 조회한 전액상환 예정금액이며,
 * 처리 시점에 재계산한 금액과 다르면 FULL_REPAYMENT_AMOUNT_CHANGED로 거절한다 (명세 7.2).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FullRepaymentRequest {

    @NotNull
    private BigDecimal expectedTotalAmount;

    @NotNull
    private String paymentMethod;

    @NotNull
    private LocalDate paymentDate;

    @NotBlank
    private String idempotencyKey;

    private String createdBy;
}
