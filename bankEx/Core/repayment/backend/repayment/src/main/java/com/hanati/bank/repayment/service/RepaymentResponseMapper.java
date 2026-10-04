package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.dto.AllocationResponse;
import com.hanati.bank.repayment.dto.RepaymentResponse;
import com.hanati.bank.repayment.dto.RepaymentTransactionResponse;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.Overpayment;
import com.hanati.bank.repayment.entity.RepaymentAllocation;
import com.hanati.bank.repayment.entity.RepaymentTransaction;
import com.hanati.bank.repayment.repository.OverpaymentRepository;
import com.hanati.bank.repayment.repository.RepaymentAllocationRepository;
import com.hanati.bank.repayment.repository.RepaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RepaymentResponseMapper {

    private final RepaymentAllocationRepository allocationRepository;
    private final RepaymentTransactionRepository transactionRepository;
    private final OverpaymentRepository overpaymentRepository;

    public RepaymentResponse toRepaymentResponse(RepaymentTransaction txn, LoanAccount account) {
        List<AllocationResponse> allocations = allocationRepository
                .findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(txn.getId()).stream()
                .map(this::toAllocationResponse)
                .toList();

        Long overpaymentId = overpaymentRepository.findByRepaymentTransactionId(txn.getId()).stream()
                .map(Overpayment::getId)
                .findFirst()
                .orElse(null);

        return new RepaymentResponse(
                txn.getTransactionNumber(),
                txn.getLoanAccountId(),
                txn.getTransactionType().name(),
                txn.getRequestedAmount(),
                txn.getAllocatedAmount(),
                txn.getOverpaymentAmount(),
                txn.getStatus().name(),
                allocations,
                account.getPrincipalBalance(),
                account.totalOutstanding(),
                account.getStatus().name(),
                overpaymentId
        );
    }

    public RepaymentTransactionResponse toTransactionResponse(RepaymentTransaction txn) {
        List<AllocationResponse> allocations = allocationRepository
                .findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(txn.getId()).stream()
                .map(this::toAllocationResponse)
                .toList();

        String originalNumber = txn.getOriginalTransactionId() == null ? null
                : transactionRepository.findById(txn.getOriginalTransactionId())
                        .map(RepaymentTransaction::getTransactionNumber)
                        .orElse(null);

        return new RepaymentTransactionResponse(
                txn.getTransactionNumber(),
                txn.getLoanAccountId(),
                txn.getTransactionType().name(),
                txn.getPaymentMethod().name(),
                txn.getRequestedAmount(),
                txn.getAllocatedAmount(),
                txn.getOverpaymentAmount(),
                txn.getStatus().name(),
                txn.getBusinessDate(),
                txn.getProcessedAt(),
                originalNumber,
                txn.getReason(),
                allocations
        );
    }

    private AllocationResponse toAllocationResponse(RepaymentAllocation a) {
        return new AllocationResponse(a.getAllocationType().name(), a.getAllocatedAmount(),
                a.getRepaymentScheduleId());
    }
}
