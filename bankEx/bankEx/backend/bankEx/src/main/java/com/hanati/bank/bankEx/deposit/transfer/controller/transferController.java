package com.hanati.bank.bankEx.deposit.transfer.controller;

import com.hanati.bank.bankEx.deposit.transfer.dto.AccountHolderResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResultResponse;
import com.hanati.bank.bankEx.deposit.transfer.service.transferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank/user/transfers")
@RequiredArgsConstructor
public class transferController {

    private final transferService transferService;

    @GetMapping("/accounts/{accountNumber}/holder")
    public ResponseEntity<AccountHolderResponse> getHolder(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transferService.getHolder(accountNumber));
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(request));
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResultResponse> getTransfer(@PathVariable Long transferId) {
        return ResponseEntity.ok(transferService.getTransfer(transferId));
    }
}
