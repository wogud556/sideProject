package com.hanati.bank.refinance.refinance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EligibilityRequest {
    private Long customerId;
    private List<Long> loanIds;
}
