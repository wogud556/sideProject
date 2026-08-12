package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.RefinanceHistoryResponse;
import com.hanati.bank.refinance.refinance.service.RefinanceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/refinance/applications")
@RequiredArgsConstructor
public class RefinanceHistoryController {

    private final RefinanceHistoryService refinanceHistoryService;

    @GetMapping("/{applicationId}/history")
    public List<RefinanceHistoryResponse> getHistory(@PathVariable Long applicationId) {
        return refinanceHistoryService.getHistory(applicationId);
    }
}
