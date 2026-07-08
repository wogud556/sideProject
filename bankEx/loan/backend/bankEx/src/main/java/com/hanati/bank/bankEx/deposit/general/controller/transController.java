package com.hanati.bank.bankEx.deposit.general.controller;

import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.dto.TransactionResponse;
import com.hanati.bank.bankEx.deposit.general.dto.WithdrawRequest;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user")
@RequiredArgsConstructor
public class transController {

    private final transService transService;

    @PostMapping("/accounts/{accountNumber}/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable String accountNumber,
                                                         @RequestBody DepositRequest request) {
        return ResponseEntity.ok(transService.deposit(accountNumber, request));
    }

    @PostMapping("/accounts/{accountNumber}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable String accountNumber,
                                                          @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(transService.withdraw(accountNumber, request));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transService.getTransactions(accountNumber));
    }
}
