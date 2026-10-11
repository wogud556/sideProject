package com.hanati.bank.screening.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionRequest {
    @NotBlank
    private String reviewerId;
    /** 승인 시에만 사용. 비우면 신청 금액 전액 승인 */
    private Long approvedAmount;
    private String comment;
}
