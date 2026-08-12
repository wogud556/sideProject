package com.hanati.bank.refinance.refinance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RepaymentInquiryRequest {
    private List<Long> loanIds;
}
