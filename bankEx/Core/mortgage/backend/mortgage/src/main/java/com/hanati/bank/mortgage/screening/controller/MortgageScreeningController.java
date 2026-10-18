package com.hanati.bank.mortgage.screening.controller;

import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.screening.service.MortgageScreeningService;
import com.hanati.bank.mortgage.screening.service.ScreeningRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages/applications/{applicationId}/screenings")
@RequiredArgsConstructor
public class MortgageScreeningController {

    private final MortgageScreeningService screeningService;
    private final MortgageApplicationQueryService queryService;

    @PostMapping
    public ResponseEntity<ApplicationDetailResponse> screen(@PathVariable Long applicationId,
                                                            @RequestHeader("X-Operator-Id") String operatorId,
                                                            @Valid @RequestBody ScreeningRequest req) {
        screeningService.screen(applicationId, operatorId, req);
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDetailResponse.ScreeningView>> history(@PathVariable Long applicationId) {
        return ResponseEntity.ok(screeningService.history(applicationId).stream()
                .map(ApplicationDetailResponse.ScreeningView::from)
                .toList());
    }
}
