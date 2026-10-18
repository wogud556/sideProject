package com.hanati.bank.mortgage.execution.controller;

import com.hanati.bank.mortgage.execution.entity.MortgageExecution;
import com.hanati.bank.mortgage.execution.service.MortgageExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mortgages/applications/{applicationId}/executions")
@RequiredArgsConstructor
public class MortgageExecutionController {

    private final MortgageExecutionService executionService;

    /** 담당자: 대출 실행. 입금 실패도 200으로 FAILED 이력을 돌려준다 */
    @PostMapping
    public ResponseEntity<MortgageExecution> execute(@PathVariable Long applicationId,
                                                     @RequestHeader("X-Operator-Id") String operatorId,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(executionService.execute(applicationId, operatorId, idempotencyKey, LocalDate.now()));
    }
}
