package com.hanati.bank.bankEx.loan.general.controller;

import com.hanati.bank.bankEx.loan.general.domain.LoanRepaymentHistory;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepayPreviewResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentDetailResponse;
import com.hanati.bank.bankEx.loan.general.service.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank/user/loans")
@RequiredArgsConstructor
public class LoanRepaymentController {

    private final LoanRepaymentService loanRepaymentService;

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<LoanRepaymentDetailResponse> repay(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanRepaymentService.repay(loanId));
    }

    @GetMapping("/{loanId}/repayment-preview")
    public ResponseEntity<LoanRepayPreviewResponse> preview(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanRepaymentService.preview(loanId));
    }

    @GetMapping("/{loanId}/repayment-history")
    public ResponseEntity<List<LoanRepaymentHistory>> history(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanRepaymentService.getHistory(loanId));
    }
}
