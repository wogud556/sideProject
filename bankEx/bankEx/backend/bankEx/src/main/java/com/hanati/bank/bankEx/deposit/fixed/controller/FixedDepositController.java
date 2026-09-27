package com.hanati.bank.bankEx.deposit.fixed.controller;

import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositAccountResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityPreviewResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositProductResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeRequest;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositTerminateResponse;
import com.hanati.bank.bankEx.deposit.fixed.service.FixedDepositScheduler;
import com.hanati.bank.bankEx.deposit.fixed.service.FixedDepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bank/user/fixed-deposits")
@RequiredArgsConstructor
public class FixedDepositController {

    private final FixedDepositService fixedDepositService;
    private final FixedDepositScheduler fixedDepositScheduler;

    @GetMapping("/products")
    public ResponseEntity<List<FixedDepositProductResponse>> getProducts() {
        return ResponseEntity.ok(fixedDepositService.getProducts());
    }

    @PostMapping
    public ResponseEntity<FixedDepositSubscribeResponse> subscribe(@RequestBody FixedDepositSubscribeRequest request) {
        return ResponseEntity.ok(fixedDepositService.subscribe(request));
    }

    @GetMapping("/{depositAccountId}")
    public ResponseEntity<FixedDepositAccountResponse> getAccount(@PathVariable String depositAccountId) {
        return ResponseEntity.ok(fixedDepositService.getAccount(depositAccountId));
    }

    @GetMapping("/{depositAccountId}/maturity-preview")
    public ResponseEntity<FixedDepositMaturityPreviewResponse> getMaturityPreview(@PathVariable String depositAccountId) {
        return ResponseEntity.ok(fixedDepositService.getMaturityPreview(depositAccountId));
    }

    @PostMapping("/{depositAccountId}/terminate")
    public ResponseEntity<FixedDepositTerminateResponse> terminate(@PathVariable String depositAccountId) {
        return ResponseEntity.ok(fixedDepositService.terminate(depositAccountId, LocalDate.now()));
    }

    @PostMapping("/scheduler/maturity")
    public ResponseEntity<FixedDepositMaturityBatchResponse> runMaturity() {
        return ResponseEntity.ok(fixedDepositScheduler.runMaturity());
    }
}
