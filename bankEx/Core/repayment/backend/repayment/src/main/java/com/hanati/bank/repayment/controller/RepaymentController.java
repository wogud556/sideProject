package com.hanati.bank.repayment.controller;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.dto.*;
import com.hanati.bank.repayment.entity.Overpayment;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import com.hanati.bank.repayment.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 명세 8번. */
@RestController
@RequestMapping("/api/loans/{loanAccountId}")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentQuoteService quoteService;
    private final RepaymentService repaymentService;
    private final RepaymentReversalService reversalService;
    private final RepaymentHistoryService historyService;
    private final OverpaymentService overpaymentService;

    /** 8.1 상환 예정금액 조회 */
    @GetMapping("/repayment-quote")
    public ResponseEntity<RepaymentQuoteResponse> quote(
            @PathVariable Long loanAccountId,
            @RequestParam(defaultValue = "REGULAR_REPAYMENT") String repaymentType,
            @RequestParam(required = false) LocalDate paymentDate,
            @RequestParam(required = false) BigDecimal requestedPrincipalAmount) {
        return ResponseEntity.ok(quoteService.quote(loanAccountId, parseType(repaymentType),
                paymentDate, requestedPrincipalAmount));
    }

    /** 8.2 상환 실행 */
    @PostMapping("/repayments")
    public ResponseEntity<RepaymentResponse> repay(@PathVariable Long loanAccountId,
                                                    @Valid @RequestBody RepaymentRequest request) {
        return ResponseEntity.ok(repaymentService.repay(loanAccountId, request));
    }

    /** 8.3 전액 상환 */
    @PostMapping("/repayments/full")
    public ResponseEntity<RepaymentResponse> repayInFull(@PathVariable Long loanAccountId,
                                                          @Valid @RequestBody FullRepaymentRequest request) {
        return ResponseEntity.ok(repaymentService.repayInFull(loanAccountId, request));
    }

    /** 8.4 상환 취소 */
    @PostMapping("/repayments/{transactionNumber}/reversal")
    public ResponseEntity<RepaymentResponse> reverse(@PathVariable Long loanAccountId,
                                                      @PathVariable String transactionNumber,
                                                      @Valid @RequestBody ReversalRequest request) {
        return ResponseEntity.ok(reversalService.reverse(loanAccountId, transactionNumber, request));
    }

    /** 8.5 상환 거래내역 조회 */
    @GetMapping("/repayments")
    public ResponseEntity<List<RepaymentTransactionResponse>> getTransactions(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(historyService.getTransactions(loanAccountId));
    }

    @GetMapping("/repayments/{transactionNumber}")
    public ResponseEntity<RepaymentTransactionResponse> getTransaction(@PathVariable Long loanAccountId,
                                                                       @PathVariable String transactionNumber) {
        return ResponseEntity.ok(historyService.getTransaction(loanAccountId, transactionNumber));
    }

    /** 8.6 과오납 환급 */
    @PostMapping("/overpayments/{overpaymentId}/refund")
    public ResponseEntity<OverpaymentRefundResponse> refund(@PathVariable Long loanAccountId,
                                                             @PathVariable Long overpaymentId) {
        return ResponseEntity.ok(overpaymentService.refund(loanAccountId, overpaymentId));
    }

    @GetMapping("/overpayments")
    public ResponseEntity<List<Overpayment>> getOverpayments(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(overpaymentService.getOverpayments(loanAccountId));
    }

    private RepaymentTransactionType parseType(String value) {
        try {
            return RepaymentTransactionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "repaymentType=" + value);
        }
    }
}
