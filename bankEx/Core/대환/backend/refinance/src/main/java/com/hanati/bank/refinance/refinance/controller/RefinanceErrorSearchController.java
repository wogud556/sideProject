package com.hanati.bank.refinance.refinance.controller;

import com.hanati.bank.refinance.refinance.dto.ErrorSearchResult;
import com.hanati.bank.refinance.refinance.service.RefinanceErrorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RefinanceErrorSearchController {

    private final RefinanceErrorSearchService refinanceErrorSearchService;

    @GetMapping("/api/refinance/errors")
    public List<ErrorSearchResult> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transactionDate,
            @RequestParam(required = false) String applicationNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String failedStep,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String status) {
        return refinanceErrorSearchService.search(transactionDate, applicationNo, customerId, failedStep, errorCode, status);
    }
}
