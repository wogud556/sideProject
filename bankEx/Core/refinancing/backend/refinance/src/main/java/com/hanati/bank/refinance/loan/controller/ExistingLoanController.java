package com.hanati.bank.refinance.loan.controller;

import com.hanati.bank.refinance.loan.dto.ExistingLoanResponse;
import com.hanati.bank.refinance.loan.service.ExistingLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExistingLoanController {

    private final ExistingLoanService existingLoanService;

    @GetMapping("/api/customers/{customerId}/loans")
    public List<ExistingLoanResponse> getByCustomer(@PathVariable Long customerId,
                                                     @RequestHeader("X-Operator-Id") String operatorId) {
        return existingLoanService.getByCustomer(customerId, operatorId);
    }

    @GetMapping("/api/loans/{loanId}")
    public ExistingLoanResponse get(@PathVariable Long loanId,
                                     @RequestHeader("X-Operator-Id") String operatorId) {
        return existingLoanService.get(loanId, operatorId);
    }
}
