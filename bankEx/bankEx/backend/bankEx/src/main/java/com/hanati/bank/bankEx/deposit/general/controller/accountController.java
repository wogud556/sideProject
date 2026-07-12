package com.hanati.bank.bankEx.deposit.general.controller;

import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseResponse;
import com.hanati.bank.bankEx.deposit.general.dto.AccountOpenRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user")
@RequiredArgsConstructor
public class accountController {

    private final accountService accountService;

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(@RequestParam String userId) {
        return ResponseEntity.ok(accountService.getAccounts(userId));
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> openAccount(@RequestBody AccountOpenRequest request) {
        return ResponseEntity.ok(accountService.openAccount(request));
    }

    @PostMapping("/accounts/{accountNumber}/close")
    public ResponseEntity<AccountCloseResponse> closeAccount(@PathVariable String accountNumber,
                                                               @RequestBody AccountCloseRequest request) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber, request));
    }
}
