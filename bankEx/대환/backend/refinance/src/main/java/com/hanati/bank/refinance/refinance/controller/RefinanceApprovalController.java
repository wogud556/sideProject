package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.ApprovalRequest;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RejectRequest;
import com.hanati.bank.refinance.refinance.service.RefinanceApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refinance/applications")
@RequiredArgsConstructor
public class RefinanceApprovalController {

    private final RefinanceApprovalService refinanceApprovalService;

    @PostMapping("/{applicationId}/approve")
    public RefinanceApplicationResponse approve(@PathVariable Long applicationId,
                                                 @RequestBody ApprovalRequest request,
                                                 @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceApprovalService.approve(applicationId, request, operatorId);
    }

    @PostMapping("/{applicationId}/reject")
    public RefinanceApplicationResponse reject(@PathVariable Long applicationId,
                                                @RequestBody RejectRequest request,
                                                @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceApprovalService.reject(applicationId, request, operatorId);
    }
}
