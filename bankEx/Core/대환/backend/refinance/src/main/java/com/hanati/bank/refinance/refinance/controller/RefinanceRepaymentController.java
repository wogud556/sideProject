package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RepaymentAmountResponse;
import com.hanati.bank.refinance.refinance.dto.RepaymentInquiryRequest;
import com.hanati.bank.refinance.refinance.service.RefinanceRepaymentInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/refinance")
@RequiredArgsConstructor
public class RefinanceRepaymentController {

    private final RefinanceRepaymentInquiryService refinanceRepaymentInquiryService;

    @PostMapping("/repayment-inquiry")
    public List<RepaymentAmountResponse> inquire(@RequestBody RepaymentInquiryRequest request) {
        return refinanceRepaymentInquiryService.inquire(request.getLoanIds());
    }
}
