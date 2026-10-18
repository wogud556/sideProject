package com.hanati.bank.mortgage.approval.controller;

import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.approval.dto.AgreementRequest;
import com.hanati.bank.mortgage.approval.dto.ApprovalRequest;
import com.hanati.bank.mortgage.approval.dto.PreconditionRequest;
import com.hanati.bank.mortgage.approval.service.MortgageAgreementService;
import com.hanati.bank.mortgage.approval.service.MortgageApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mortgages/applications/{applicationId}")
@RequiredArgsConstructor
public class MortgageApprovalController {

    private final MortgageApprovalService approvalService;
    private final MortgageAgreementService agreementService;
    private final MortgageApplicationQueryService queryService;

    /** 담당자: 대출 승인 */
    @PostMapping("/approvals")
    public ResponseEntity<ApplicationDetailResponse> approve(@PathVariable Long applicationId,
                                                             @RequestHeader("X-Operator-Id") String operatorId,
                                                             @Valid @RequestBody ApprovalRequest req) {
        approvalService.approve(applicationId, operatorId, req, LocalDate.now());
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }

    /** 고객 채널: 약정 */
    @PostMapping("/agreements")
    public ResponseEntity<ApplicationDetailResponse> agree(@PathVariable Long applicationId,
                                                           @Valid @RequestBody AgreementRequest req) {
        agreementService.agree(applicationId, req, LocalDate.now());
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }

    /** 담당자: 담보권 설정·조건 이행 확인 */
    @PostMapping("/execution-preconditions")
    public ResponseEntity<ApplicationDetailResponse> confirmPreconditions(@PathVariable Long applicationId,
                                                                          @RequestHeader("X-Operator-Id") String operatorId,
                                                                          @Valid @RequestBody PreconditionRequest req) {
        agreementService.confirmPreconditions(applicationId, operatorId, req);
        return ResponseEntity.ok(queryService.getDetail(applicationId));
    }
}
