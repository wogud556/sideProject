package com.hanati.bank.mortgage.application.controller;

import com.hanati.bank.mortgage.application.dto.ApplicationCreateRequest;
import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.dto.ApplicationSummary;
import com.hanati.bank.mortgage.application.dto.CustomerActionRequest;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.application.service.MortgageApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages")
@RequiredArgsConstructor
public class MortgageApplicationController {

    private final MortgageApplicationService applicationService;
    private final MortgageApplicationQueryService queryService;

    /** 고객 채널: 신청 */
    @PostMapping("/applications")
    public ResponseEntity<ApplicationDetailResponse> apply(@Valid @RequestBody ApplicationCreateRequest req) {
        Long id = applicationService.apply(req, LocalDate.now()).getApplicationId();
        return ResponseEntity.ok(queryService.getDetail(id));
    }

    /** 담당자: 업무 대기열 */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationSummary>> getApplications(@RequestParam(required = false) ApplicationStatus status) {
        return ResponseEntity.ok(queryService.getApplications(status));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationDetailResponse> getApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }

    /** 고객 채널: 내 신청 목록 */
    @GetMapping("/customers/{customerId}/applications")
    public ResponseEntity<List<ApplicationSummary>> getCustomerApplications(@PathVariable String customerId) {
        return ResponseEntity.ok(queryService.getCustomerApplications(customerId));
    }

    /** 고객 채널: 신청 철회 */
    @PostMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<ApplicationDetailResponse> withdraw(@PathVariable Long applicationId,
                                                              @Valid @RequestBody CustomerActionRequest req) {
        applicationService.withdraw(applicationId, req.customerId());
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }
}
