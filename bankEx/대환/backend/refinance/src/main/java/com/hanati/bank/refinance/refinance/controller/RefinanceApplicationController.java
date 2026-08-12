package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplyRequest;
import com.hanati.bank.refinance.refinance.service.RefinanceApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/refinance/applications")
@RequiredArgsConstructor
public class RefinanceApplicationController {

    private final RefinanceApplicationService refinanceApplicationService;

    @PostMapping
    public RefinanceApplicationResponse apply(@RequestBody RefinanceApplyRequest request,
                                               @RequestHeader("X-Operator-Id") String operatorId) {
        return refinanceApplicationService.apply(request, operatorId);
    }

    @GetMapping
    public List<RefinanceApplicationResponse> list() {
        return refinanceApplicationService.list();
    }

    @GetMapping("/{applicationId}")
    public RefinanceApplicationResponse get(@PathVariable Long applicationId) {
        return refinanceApplicationService.get(applicationId);
    }
}
