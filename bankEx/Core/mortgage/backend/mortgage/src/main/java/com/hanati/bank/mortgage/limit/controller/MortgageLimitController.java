package com.hanati.bank.mortgage.limit.controller;

import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.limit.service.LimitCalculationRequest;
import com.hanati.bank.mortgage.limit.service.MortgageLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mortgages/applications/{applicationId}")
@RequiredArgsConstructor
public class MortgageLimitController {

    private final MortgageLimitService limitService;
    private final MortgageApplicationQueryService queryService;

    @PostMapping("/limits/calculate")
    public ResponseEntity<ApplicationDetailResponse> calculate(@PathVariable Long applicationId,
                                                               @RequestHeader("X-Operator-Id") String operatorId,
                                                               @Valid @RequestBody LimitCalculationRequest req) {
        limitService.calculate(applicationId, operatorId, req, LocalDate.now());
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }
}
