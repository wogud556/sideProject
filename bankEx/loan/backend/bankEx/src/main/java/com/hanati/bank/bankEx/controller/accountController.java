package com.hanati.bank.bankEx.controller;

import com.hanati.bank.bankEx.dto.AccountResponse;
import com.hanati.bank.bankEx.service.accountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
