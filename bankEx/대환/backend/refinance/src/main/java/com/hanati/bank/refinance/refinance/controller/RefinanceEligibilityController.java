package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.EligibilityRequest;
import com.hanati.bank.refinance.refinance.dto.EligibilityResponse;
import com.hanati.bank.refinance.refinance.service.RefinanceEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refinance")
@RequiredArgsConstructor
public class RefinanceEligibilityController {

    private final RefinanceEligibilityService refinanceEligibilityService;

    @PostMapping("/eligibility")
    public EligibilityResponse checkEligibility(@RequestBody EligibilityRequest request) {
        return refinanceEligibilityService.check(request.getCustomerId(), request.getLoanIds());
    }
}
