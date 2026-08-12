package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.service.RefinanceRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refinance/applications")
@RequiredArgsConstructor
public class RefinanceRetryController {

    private final RefinanceRetryService refinanceRetryService;

    @PostMapping("/{applicationId}/retry")
    public RefinanceApplicationResponse retry(@PathVariable Long applicationId,
                                               @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceRetryService.retry(applicationId, operatorId);
    }
}
