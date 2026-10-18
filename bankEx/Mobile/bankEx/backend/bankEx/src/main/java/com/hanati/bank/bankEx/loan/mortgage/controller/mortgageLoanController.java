package com.hanati.bank.bankEx.loan.mortgage.controller;

import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.Installment;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.LoanAccount;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.Product;
import com.hanati.bank.bankEx.loan.mortgage.dto.*;
import com.hanati.bank.bankEx.loan.mortgage.service.mortgageLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 주담대 고객 API. 심사·승인·실행 같은 담당자 업무는 Core/mortgage 담당자 화면에서만 한다. */
@RestController
@RequestMapping("/api/bank/user/loan/mortgage")
@RequiredArgsConstructor
public class mortgageLoanController {

    private final mortgageLoanService mortgageLoanService;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(mortgageLoanService.getProducts());
    }

    @PostMapping("/applications")
    public ResponseEntity<MortgageApplicationResponse> apply(@RequestBody MortgageApplyRequest request) {
        return ResponseEntity.ok(mortgageLoanService.apply(request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<MortgageApplicationResponse>> getApplications(@RequestParam String userId) {
        return ResponseEntity.ok(mortgageLoanService.getApplications(userId));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<MortgageApplicationResponse> getApplication(@PathVariable Long applicationId,
                                                                      @RequestParam String userId) {
        return ResponseEntity.ok(mortgageLoanService.getApplication(applicationId, userId));
    }

    @PostMapping("/applications/{applicationId}/agreement")
    public ResponseEntity<MortgageApplicationResponse> agree(@PathVariable Long applicationId,
                                                             @RequestBody MortgageAgreementRequest request) {
        return ResponseEntity.ok(mortgageLoanService.agree(applicationId, request));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<MortgageApplicationResponse> withdraw(@PathVariable Long applicationId,
                                                                @RequestBody MortgageUserRequest request) {
        return ResponseEntity.ok(mortgageLoanService.withdraw(applicationId, request.userId()));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<LoanAccount>> getLoanAccounts(@RequestParam String userId) {
        return ResponseEntity.ok(mortgageLoanService.getLoanAccounts(userId));
    }

    @GetMapping("/accounts/{loanAccountId}")
    public ResponseEntity<LoanAccount> getLoanAccount(@PathVariable Long loanAccountId, @RequestParam String userId) {
        return ResponseEntity.ok(mortgageLoanService.getLoanAccount(loanAccountId, userId));
    }

    @GetMapping("/accounts/{loanAccountId}/schedule")
    public ResponseEntity<List<Installment>> getSchedule(@PathVariable Long loanAccountId, @RequestParam String userId) {
        return ResponseEntity.ok(mortgageLoanService.getSchedule(loanAccountId, userId));
    }

    @PostMapping("/accounts/{loanAccountId}/repayments")
    public ResponseEntity<MortgageRepayResponse> repay(@PathVariable Long loanAccountId,
                                                       @RequestBody MortgageRepayRequest request) {
        return ResponseEntity.ok(mortgageLoanService.repay(loanAccountId, request));
    }
}
