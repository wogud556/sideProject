package com.hanati.bank.mortgage.repayment.controller;

import com.hanati.bank.mortgage.repayment.dto.LoanAccountResponse;
import com.hanati.bank.mortgage.repayment.dto.RepaymentRequest;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentHistory;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;
import com.hanati.bank.mortgage.repayment.service.MortgageRepaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages")
@RequiredArgsConstructor
public class MortgageRepaymentController {

    private final MortgageRepaymentService repaymentService;

    @GetMapping("/accounts/{loanAccountId}")
    public ResponseEntity<LoanAccountResponse> getAccount(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(repaymentService.getAccount(loanAccountId));
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<LoanAccountResponse>> getCustomerAccounts(@PathVariable String customerId) {
        return ResponseEntity.ok(repaymentService.getCustomerAccounts(customerId));
    }

    /** 상환 예정표 */
    @GetMapping("/accounts/{loanAccountId}/repayments")
    public ResponseEntity<List<MortgageRepaymentSchedule>> getSchedule(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(repaymentService.getSchedule(loanAccountId));
    }

    @GetMapping("/accounts/{loanAccountId}/repayment-history")
    public ResponseEntity<List<MortgageRepaymentHistory>> getHistory(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(repaymentService.getHistory(loanAccountId));
    }

    /** 고객 채널: 다음 회차 상환 */
    @PostMapping("/accounts/{loanAccountId}/repayments")
    public ResponseEntity<MortgageRepaymentHistory> repay(@PathVariable Long loanAccountId,
                                                          @Valid @RequestBody RepaymentRequest req) {
        return ResponseEntity.ok(repaymentService.repay(loanAccountId, req));
    }
}
