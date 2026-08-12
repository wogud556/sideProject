package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.service.RefinanceExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refinance/applications")
@RequiredArgsConstructor
public class RefinanceExecutionController {

    private final RefinanceExecutionService refinanceExecutionService;

    @PostMapping("/{applicationId}/execute")
    public RefinanceApplicationResponse execute(@PathVariable Long applicationId,
                                                 @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceExecutionService.execute(applicationId, operatorId);
    }
}
