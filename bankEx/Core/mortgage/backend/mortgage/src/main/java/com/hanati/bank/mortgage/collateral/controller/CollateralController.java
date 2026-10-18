package com.hanati.bank.mortgage.collateral.controller;

import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.collateral.dto.CollateralEvaluationRequest;
import com.hanati.bank.mortgage.collateral.service.CollateralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mortgages/applications/{applicationId}")
@RequiredArgsConstructor
public class CollateralController {

    private final CollateralService collateralService;
    private final MortgageApplicationQueryService queryService;

    @PostMapping("/collaterals")
    public ResponseEntity<ApplicationDetailResponse> evaluate(@PathVariable Long applicationId,
                                                              @RequestHeader("X-Operator-Id") String operatorId,
                                                              @Valid @RequestBody CollateralEvaluationRequest req) {
        collateralService.evaluate(applicationId, operatorId, req, LocalDate.now());
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }
}
