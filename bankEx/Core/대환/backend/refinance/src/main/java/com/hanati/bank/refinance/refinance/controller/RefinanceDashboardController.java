package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.DashboardResponse;
import com.hanati.bank.refinance.refinance.service.RefinanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RefinanceDashboardController {

    private final RefinanceDashboardService refinanceDashboardService;

    @GetMapping("/api/refinance/dashboard")
    public DashboardResponse getDashboard() {
        return refinanceDashboardService.getDashboard();
    }
}
