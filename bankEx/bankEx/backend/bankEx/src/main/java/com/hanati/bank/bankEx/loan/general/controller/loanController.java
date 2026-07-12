package com.hanati.bank.bankEx.loan.general.controller;

import com.hanati.bank.bankEx.loan.general.dto.LoanAccountValidateResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanProductResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentResponse;
import com.hanati.bank.bankEx.loan.general.service.loanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user")
@RequiredArgsConstructor
public class loanController {

    private final loanService loanService;

    @GetMapping("/loan/products")
    public ResponseEntity<List<LoanProductResponse>> getProducts() {
        return ResponseEntity.ok(loanService.getProducts());
    }

    @GetMapping("/loan/products/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        try {
            return ResponseEntity.ok(loanService.getProduct(productId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/loan/apply")
    public ResponseEntity<?> applyLoan(@RequestBody LoanApplicationRequest request) {
        try {
            LoanApplicationResponse response = loanService.applyLoan(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/loan/applications")
    public ResponseEntity<List<LoanApplicationResponse>> getMyApplications(@RequestParam String userId) {
        return ResponseEntity.ok(loanService.getMyApplications(userId));
    }

    @GetMapping("/loan/validate-customer-account")
    public ResponseEntity<LoanAccountValidateResponse> validateCustomerAccount(@RequestParam String userId,
                                                                                @RequestParam String accountNumber) {
        return ResponseEntity.ok(loanService.validateCustomerAccount(userId, accountNumber));
    }

    @PostMapping("/loan/applications/{applicationId}/approve")
    public ResponseEntity<LoanApplicationResponse> approveApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(loanService.approveApplication(applicationId));
    }

    @PostMapping("/loan/disbursement")
    public ResponseEntity<LoanDisbursementResponse> disburse(@RequestBody LoanDisbursementRequest request) {
        return ResponseEntity.ok(loanService.disburse(request));
    }

    @PostMapping("/loan/repayment")
    public ResponseEntity<LoanRepaymentResponse> repay(@RequestBody LoanRepaymentRequest request) {
        return ResponseEntity.ok(loanService.repay(request));
    }
}
