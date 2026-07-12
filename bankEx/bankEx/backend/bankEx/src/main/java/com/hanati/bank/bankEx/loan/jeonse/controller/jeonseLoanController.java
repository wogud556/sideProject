package com.hanati.bank.bankEx.loan.jeonse.controller;

import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanApplyRequest;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanApplyResponse;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanExecuteResponse;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanProductResponse;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanReviewResponse;
import com.hanati.bank.bankEx.loan.jeonse.service.jeonseLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user")
@RequiredArgsConstructor
public class jeonseLoanController {

    private final jeonseLoanService jeonseLoanService;

    @GetMapping("/loan/jeonse/products")
    public ResponseEntity<List<JeonseLoanProductResponse>> getProducts() {
        return ResponseEntity.ok(jeonseLoanService.getProducts());
    }

    @PostMapping("/loan/jeonse/applications")
    public ResponseEntity<JeonseLoanApplyResponse> apply(@RequestBody JeonseLoanApplyRequest request) {
        return ResponseEntity.ok(jeonseLoanService.apply(request));
    }

    @PostMapping("/loan/jeonse/applications/{applicationId}/review")
    public ResponseEntity<JeonseLoanReviewResponse> review(@PathVariable String applicationId) {
        return ResponseEntity.ok(jeonseLoanService.review(applicationId));
    }

    @PostMapping("/loan/jeonse/applications/{applicationId}/execute")
    public ResponseEntity<JeonseLoanExecuteResponse> execute(@PathVariable String applicationId) {
        return ResponseEntity.ok(jeonseLoanService.execute(applicationId));
    }
}
