package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.ReviewRequest;
import com.hanati.bank.refinance.refinance.service.RefinanceReviewService;
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
public class RefinanceReviewController {

    private final RefinanceReviewService refinanceReviewService;

    @PostMapping("/{applicationId}/review")
    public RefinanceApplicationResponse review(@PathVariable Long applicationId,
                                                @RequestBody ReviewRequest request,
                                                @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceReviewService.review(applicationId, request.getOpinion(), operatorId);
    }
}
