package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.common.util.TransactionNumberGenerator;
import com.hanati.bank.repayment.dto.RepaymentResponse;
import com.hanati.bank.repayment.dto.ReversalRequest;
import com.hanati.bank.repayment.entity.*;
import com.hanati.bank.repayment.enums.*;
import com.hanati.bank.repayment.policy.AllocationLine;
import com.hanati.bank.repayment.policy.RepaymentAllocationResult;
import com.hanati.bank.repayment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상환 취소 (명세 3.7 / 7.3). 원거래를 삭제하거나 금액을 수정하지 않고,
 * 원거래 배분 내역을 음수로 뒤집은 역배분 행을 추가해 잔액을 복구한다.
 */
@Service
@RequiredArgsConstructor
public class RepaymentReversalService {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final RepaymentTransactionRepository transactionRepository;
    private final RepaymentAllocationRepository allocationRepository;
    private final OverpaymentRepository overpaymentRepository;
    private final DebtBalanceApplier balanceApplier;
    private final AccountingEventPublisher accountingEventPublisher;
    private final RepaymentResponseMapper responseMapper;

    @Transactional
    public RepaymentResponse reverse(Long loanAccountId, String transactionNumber, ReversalRequest request) {
        Optional<RepaymentTransaction> replayed =
                transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (replayed.isPresent()) {
            RepaymentTransaction existing = replayed.get();
            if (existing.getTransactionType() != RepaymentTransactionType.REPAYMENT_REVERSAL) {
                throw new BusinessException(ErrorCode.DUPLICATE_REPAYMENT_REQUEST, request.getIdempotencyKey());
            }
            LoanAccount account = loanAccountRepository.findById(existing.getLoanAccountId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
            return responseMapper.toRepaymentResponse(existing, account);
        }

        // 1) 원거래 조회
        RepaymentTransaction original = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAYMENT_TRANSACTION_NOT_FOUND));

        // 다른 대출계좌의 거래를 취소할 수 없다 (명세 9번)
        if (!original.getLoanAccountId().equals(loanAccountId)) {
            throw new BusinessException(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED,
                    "거래가 대출계좌 " + loanAccountId + "에 속하지 않습니다");
        }

        // 2~3) 완료 상태 및 기존 취소 여부
        if (original.getStatus() == RepaymentTransactionStatus.REVERSED) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALREADY_REVERSED);
        }
        if (original.getStatus() != RepaymentTransactionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED,
                    "원거래 상태: " + original.getStatus().name());
        }
        if (original.getTransactionType() == RepaymentTransactionType.REPAYMENT_REVERSAL) {
            throw new BusinessException(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED, "취소 거래는 취소할 수 없습니다");
        }

        LoanAccount account = loanAccountRepository.findByIdForUpdate(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));

        // 4~5) 후속 거래가 있으면 원상 복구가 보장되지 않으므로 거절한다
        if (transactionRepository.existsByLoanAccountIdAndStatusAndIdGreaterThan(
                loanAccountId, RepaymentTransactionStatus.COMPLETED, original.getId())) {
            throw new BusinessException(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED,
                    "원거래 이후 완료된 상환 거래가 존재합니다");
        }

        // 환급이 끝난 과오납은 돈이 이미 나갔으므로 되돌릴 수 없다
        List<Overpayment> overpayments = overpaymentRepository.findByRepaymentTransactionId(original.getId());
        for (Overpayment o : overpayments) {
            if (o.getStatus() == OverpaymentStatus.REFUNDED) {
                throw new BusinessException(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED,
                        "과오납 " + o.getId() + "이 이미 환급되었습니다");
            }
        }

        // 6) 원거래 배분 내역으로 역배분 생성
        List<RepaymentAllocation> originalAllocations = allocationRepository
                .findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(original.getId());
        List<AllocationLine> originalLines = originalAllocations.stream()
                .map(a -> new AllocationLine(a.getRepaymentScheduleId(), a.getAllocationType(),
                        a.getAllocatedAmount(), a.getAllocationOrder()))
                .toList();

        List<RepaymentSchedule> schedules = scheduleRepository
                .findByLoanAccountIdOrderByDueDateAscInstallmentNumberAscIdAsc(loanAccountId);
        Map<Long, RepaymentSchedule> byId = schedules.stream()
                .collect(Collectors.toMap(RepaymentSchedule::getId, Function.identity()));

        // 7~8) 잔액 및 스케줄 상태 복구 (sign = -1)
        balanceApplier.apply(account, byId, schedules, originalLines, original.getBusinessDate(), -1);
        balanceApplier.restoreFeeBalance(account,
                totalOf(originalLines, AllocationType.FEE), original.getPrepaymentFee());

        // 대출계좌 상태를 거래 직전 값으로 복원
        account.setStatus(original.getAccountStatusBefore() != null
                ? original.getAccountStatusBefore()
                : LoanAccountStatus.ACTIVE);

        // 9) 원거래를 REVERSED로
        original.setStatus(RepaymentTransactionStatus.REVERSED);

        // 10) 취소 거래 및 역배분 저장. 금액은 모두 음수로 적어 "요청 = 배분 + 과오납"이 그대로 성립한다.
        LocalDate businessDate = LocalDate.now();
        RepaymentTransaction reversal = transactionRepository.save(RepaymentTransaction.builder()
                .transactionNumber(TransactionNumberGenerator.generate(businessDate,
                        transactionRepository::existsByTransactionNumber))
                .loanAccountId(loanAccountId)
                .transactionType(RepaymentTransactionType.REPAYMENT_REVERSAL)
                .paymentMethod(PaymentMethod.INTERNAL)
                .requestedAmount(original.getRequestedAmount().negate())
                .allocatedAmount(original.getAllocatedAmount().negate())
                .overpaymentAmount(original.getOverpaymentAmount().negate())
                .prepaymentFee(Money.ZERO)
                .status(RepaymentTransactionStatus.COMPLETED)
                .businessDate(businessDate)
                .processedAt(LocalDateTime.now())
                .idempotencyKey(request.getIdempotencyKey())
                .originalTransactionId(original.getId())
                .accountStatusBefore(account.getStatus())
                .createdBy(request.getCreatedBy())
                .reason(request.getReason())
                .build());

        List<RepaymentAllocation> reverseRows = originalLines.stream()
                .map(l -> RepaymentAllocation.builder()
                        .repaymentTransactionId(reversal.getId())
                        .repaymentScheduleId(l.scheduleId())
                        .allocationType(l.type())
                        .allocatedAmount(l.amount().negate())
                        .allocationOrder(l.order())
                        .build())
                .toList();
        allocationRepository.saveAll(reverseRows);

        // 과오납은 환급 대기 상태에서만 취소된다
        for (Overpayment o : overpayments) {
            if (o.getStatus() == OverpaymentStatus.PENDING_REFUND
                    || o.getStatus() == OverpaymentStatus.REFUND_FAILED) {
                o.setStatus(OverpaymentStatus.REFUND_CANCELLED);
            }
        }
        overpaymentRepository.saveAll(overpayments);

        // 11) 반대 회계 이벤트
        accountingEventPublisher.publishRepayment(reversal, new RepaymentAllocationResult(originalLines), -1);

        account.setLastTransactionDate(reversal.getProcessedAt());
        scheduleRepository.saveAll(schedules);
        loanAccountRepository.save(account);
        transactionRepository.save(original);

        return responseMapper.toRepaymentResponse(reversal, account);
    }

    private BigDecimal totalOf(List<AllocationLine> lines, AllocationType type) {
        return lines.stream()
                .filter(l -> l.type() == type)
                .map(AllocationLine::amount)
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
