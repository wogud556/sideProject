package com.hanati.bank.bankEx.deposit.savings.controller;

import com.hanati.bank.bankEx.deposit.savings.dto.AutoTransferBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.MaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsAccountResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsCancelResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsPaymentRequest;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsPaymentResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsProductResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeRequest;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeResponse;
import com.hanati.bank.bankEx.deposit.savings.service.savingsAccountService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsCancelService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsProductService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user/deposit/savings")
@RequiredArgsConstructor
public class savingsController {

    private final savingsProductService savingsProductService;
    private final savingsAccountService savingsAccountService;
    private final savingsCancelService savingsCancelService;
    private final savingsScheduler savingsScheduler;

    @GetMapping("/products")
    public ResponseEntity<List<SavingsProductResponse>> getProducts() {
        return ResponseEntity.ok(savingsProductService.getProducts());
    }

    @PostMapping("/accounts")
    public ResponseEntity<SavingsSubscribeResponse> subscribe(@RequestBody SavingsSubscribeRequest request) {
        return ResponseEntity.ok(savingsAccountService.subscribe(request));
    }

    @GetMapping("/accounts/{accountNo}")
    public ResponseEntity<SavingsAccountResponse> getAccount(@PathVariable String accountNo) {
        return ResponseEntity.ok(savingsAccountService.getAccount(accountNo));
    }

    @PostMapping("/accounts/{accountNo}/payment")
    public ResponseEntity<SavingsPaymentResponse> pay(@PathVariable String accountNo,
                                                        @RequestBody(required = false) SavingsPaymentRequest request) {
        return ResponseEntity.ok(savingsAccountService.pay(accountNo, request));
    }

    @PostMapping("/accounts/{accountNo}/cancel")
    public ResponseEntity<SavingsCancelResponse> cancel(@PathVariable String accountNo) {
        return ResponseEntity.ok(savingsCancelService.cancel(accountNo));
    }

    @PostMapping("/scheduler/auto-transfer")
    public ResponseEntity<AutoTransferBatchResponse> runAutoTransfer() {
        return ResponseEntity.ok(savingsScheduler.runAutoTransfer());
    }

    @PostMapping("/scheduler/maturity")
    public ResponseEntity<MaturityBatchResponse> runMaturity() {
        return ResponseEntity.ok(savingsScheduler.runMaturity());
    }
}
