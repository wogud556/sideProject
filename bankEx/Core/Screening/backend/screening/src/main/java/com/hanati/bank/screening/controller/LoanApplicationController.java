package com.hanati.bank.screening.controller;

import com.hanati.bank.screening.dto.LoanApplicationRequest;
import com.hanati.bank.screening.dto.LoanApplicationResponse;
import com.hanati.bank.screening.dto.ScreeningResponse;
import com.hanati.bank.screening.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening/applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> apply(@Valid @RequestBody LoanApplicationRequest req) {
        return ResponseEntity.ok(loanApplicationService.apply(req));
    }

    @PostMapping("/{applicationId}/screening")
    public ResponseEntity<ScreeningResponse> screen(@PathVariable Long applicationId) {
        return ResponseEntity.ok(loanApplicationService.screen(applicationId));
    }

    @GetMapping("/my/{userId}")
    public ResponseEntity<List<LoanApplicationResponse>> getMyApplications(@PathVariable String userId) {
        return ResponseEntity.ok(loanApplicationService.getMyApplications(userId));
    }

    @GetMapping("/{applicationId}/result")
    public ResponseEntity<ScreeningResponse> getResult(@PathVariable Long applicationId) {
        return ResponseEntity.ok(loanApplicationService.getScreeningResult(applicationId));
    }
}
