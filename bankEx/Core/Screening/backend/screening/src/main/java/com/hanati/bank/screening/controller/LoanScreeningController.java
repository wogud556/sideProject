package com.hanati.bank.screening.controller;

import com.hanati.bank.screening.dto.ReviewDecisionRequest;
import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.dto.ScreeningResponse;
import com.hanati.bank.screening.enums.ScreeningStatus;
import com.hanati.bank.screening.service.LoanScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings")
@RequiredArgsConstructor
public class LoanScreeningController {

    private final LoanScreeningService loanScreeningService;

    @PostMapping("/loans")
    public ResponseEntity<ScreeningResponse> screen(@Valid @RequestBody ScreeningRequest req) {
        return ResponseEntity.ok(loanScreeningService.screen(req));
    }

    @GetMapping
    public ResponseEntity<List<ScreeningResponse>> getScreenings(@RequestParam(required = false) ScreeningStatus status) {
        return ResponseEntity.ok(loanScreeningService.getScreenings(status));
    }

    @GetMapping("/{screeningId}")
    public ResponseEntity<ScreeningResponse> getScreening(@PathVariable Long screeningId) {
        return ResponseEntity.ok(loanScreeningService.getScreening(screeningId));
    }

    @PostMapping("/{screeningId}/approve")
    public ResponseEntity<ScreeningResponse> approve(@PathVariable Long screeningId,
                                                     @Valid @RequestBody ReviewDecisionRequest req) {
        return ResponseEntity.ok(loanScreeningService.approve(screeningId, req));
    }

    @PostMapping("/{screeningId}/reject")
    public ResponseEntity<ScreeningResponse> reject(@PathVariable Long screeningId,
                                                    @Valid @RequestBody ReviewDecisionRequest req) {
        return ResponseEntity.ok(loanScreeningService.reject(screeningId, req));
    }
}
