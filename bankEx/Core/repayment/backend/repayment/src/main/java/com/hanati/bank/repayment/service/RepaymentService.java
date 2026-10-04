package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.common.util.TransactionNumberGenerator;
import com.hanati.bank.repayment.dto.FullRepaymentRequest;
import com.hanati.bank.repayment.dto.RepaymentRequest;
import com.hanati.bank.repayment.dto.RepaymentResponse;
import com.hanati.bank.repayment.entity.*;
import com.hanati.bank.repayment.enums.*;
import com.hanati.bank.repayment.gateway.WithdrawalGateway;
import com.hanati.bank.repayment.gateway.dto.WithdrawalRequest;
import com.hanati.bank.repayment.gateway.dto.WithdrawalResult;
import com.hanati.bank.repayment.policy.*;
import com.hanati.bank.repayment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 상환 실행 (명세 7.1 일반 상환, 7.2 전액 상환). 전 과정을 하나의 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class RepaymentService {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final RepaymentTransactionRepository transactionRepository;
    private final RepaymentAllocationRepository allocationRepository;
    private final OverpaymentRepository overpaymentRepository;
    private final RepaymentAllocationPolicySelector policySelector;
    private final RepaymentQuoteService quoteService;
    private final DebtBalanceApplier balanceApplier;
    private final AccountingEventPublisher accountingEventPublisher;
    private final RepaymentResponseMapper responseMapper;
    private final WithdrawalGateway withdrawalGateway;

    @Transactional
    public RepaymentResponse repay(Long loanAccountId, RepaymentRequest request) {
        RepaymentTransactionType type = parseType(request.getRepaymentType());
        if (type == RepaymentTransactionType.REPAYMENT_REVERSAL
                || type == RepaymentTransactionType.OVERPAYMENT_REFUND
                || type == RepaymentTransactionType.FULL_REPAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    request.getRepaymentType() + "은 전용 API를 사용해야 합니다");
        }
        PaymentMethod method = parseMethod(request.getPaymentMethod());

        // 2) 멱등성 키 확인
        Optional<RepaymentResponse> replayed = replay(request.getIdempotencyKey(), type, request.getAmount());
        if (replayed.isPresent()) {
            return replayed.get();
        }

        // 3) 대출계좌 조회 및 잠금
        LoanAccount account = lockAccount(loanAccountId);
        List<RepaymentSchedule> schedules = loadSchedules(loanAccountId);
        LocalDate businessDate = resolveBusinessDate(request.getPaymentDate(), account);

        BigDecimal amount = resolveAmount(request.getAmount(), account, schedules, type, businessDate);
        BigDecimal prepaymentFee = type == RepaymentTransactionType.PREPAYMENT
                ? quoteService.prepaymentFee(account, prepaymentPrincipalOf(amount, account))
                : Money.ZERO;

        if (method == PaymentMethod.AUTO_DEBIT) {
            withdraw(account, amount);
        }

        return execute(account, schedules, type, method, amount, prepaymentFee, businessDate,
                request.getIdempotencyKey(), request.getCreatedBy(), null);
    }

    /** 전액 상환 (명세 7.2). 처리 시점에 금액을 재계산해 조회 당시 금액과 비교한다. */
    @Transactional
    public RepaymentResponse repayInFull(Long loanAccountId, FullRepaymentRequest request) {
        Optional<RepaymentResponse> replayed = replay(request.getIdempotencyKey(),
                RepaymentTransactionType.FULL_REPAYMENT, request.getExpectedTotalAmount());
        if (replayed.isPresent()) {
            return replayed.get();
        }

        PaymentMethod method = parseMethod(request.getPaymentMethod());
        LoanAccount account = lockAccount(loanAccountId);
        List<RepaymentSchedule> schedules = loadSchedules(loanAccountId);
        LocalDate businessDate = resolveBusinessDate(request.getPaymentDate(), account);

        BigDecimal recalculated = quoteService.fullRepaymentTotal(account, schedules, businessDate);
        if (recalculated.compareTo(Money.normalize(request.getExpectedTotalAmount())) != 0) {
            throw new BusinessException(ErrorCode.FULL_REPAYMENT_AMOUNT_CHANGED,
                    "재계산 금액 " + recalculated.toPlainString());
        }

        BigDecimal prepaymentFee = quoteService.prepaymentFee(account, account.getPrincipalBalance());

        if (method == PaymentMethod.AUTO_DEBIT) {
            withdraw(account, recalculated);
        }

        return execute(account, schedules, RepaymentTransactionType.FULL_REPAYMENT, method, recalculated,
                prepaymentFee, businessDate, request.getIdempotencyKey(), request.getCreatedBy(), null);
    }

    /** 배분 → 잔액 반영 → 원장 저장 → 회계 이벤트 → 완제 판단 (명세 7.1의 6~13단계). */
    RepaymentResponse execute(LoanAccount account, List<RepaymentSchedule> schedules,
                               RepaymentTransactionType type, PaymentMethod method, BigDecimal amount,
                               BigDecimal prepaymentFee, LocalDate businessDate, String idempotencyKey,
                               String createdBy, String reason) {

        if (!Money.isPositive(amount)) {
            throw new BusinessException(ErrorCode.INVALID_REPAYMENT_AMOUNT);
        }
        if (!account.isRepayable()) {
            throw new BusinessException(ErrorCode.INVALID_LOAN_STATUS,
                    "현재 상태: " + account.getStatus().name());
        }

        // 6) 배분 정책 선택  7) 배분
        RepaymentAllocationPolicy policy = policySelector.select(account.getProductType());
        RepaymentAllocationResult result = policy.allocate(RepaymentAllocationContext.builder()
                .transactionType(type)
                .businessDate(businessDate)
                .paymentAmount(amount)
                .feeBalance(account.getFeeBalance())
                .prepaymentFee(prepaymentFee)
                .principalBalance(account.getPrincipalBalance())
                .schedules(toSnapshots(schedules))
                .build());

        // 명세 13.4: 요청금액 = 배분금액 합계 + 과오납금액
        if (result.total().compareTo(amount) != 0) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALLOCATION_FAILED,
                    "요청 " + amount.toPlainString() + " != 배분 " + result.total().toPlainString());
        }

        // 8~9) 계좌 잔액 및 스케줄 상태 갱신
        Map<Long, RepaymentSchedule> byId = schedules.stream()
                .collect(Collectors.toMap(RepaymentSchedule::getId, Function.identity()));
        balanceApplier.apply(account, byId, schedules, result.getLines(), businessDate, 1);

        // 10) 거래 및 배분 내역 저장 (취소 복원을 위해 거래 직전 계좌 상태를 함께 남긴다)
        LoanAccountStatus statusBefore = account.getStatus();
        RepaymentTransaction txn = transactionRepository.save(RepaymentTransaction.builder()
                .transactionNumber(TransactionNumberGenerator.generate(businessDate,
                        transactionRepository::existsByTransactionNumber))
                .loanAccountId(account.getId())
                .transactionType(type)
                .paymentMethod(method)
                .requestedAmount(amount)
                .allocatedAmount(result.getAllocatedAmount())
                .overpaymentAmount(result.getOverpaymentAmount())
                .prepaymentFee(Money.normalize(prepaymentFee))
                .status(RepaymentTransactionStatus.COMPLETED)
                .businessDate(businessDate)
                .processedAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .accountStatusBefore(statusBefore)
                .createdBy(createdBy)
                .reason(reason)
                .build());
        saveAllocations(txn, result.getLines());

        if (Money.isPositive(result.getOverpaymentAmount())) {
            overpaymentRepository.save(Overpayment.builder()
                    .loanAccountId(account.getId())
                    .repaymentTransactionId(txn.getId())
                    .amount(result.getOverpaymentAmount())
                    .status(OverpaymentStatus.PENDING_REFUND)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 11) 회계 이벤트
        accountingEventPublisher.publishRepayment(txn, result, 1);

        // 12) 완제 판단 — 모든 잔존 채무가 0원일 때만 완제로 바꾼다 (명세 3.6)
        account.setLastTransactionDate(txn.getProcessedAt());
        if (Money.isZero(account.totalOutstanding())) {
            account.setStatus(LoanAccountStatus.PAID_OFF);
            accountingEventPublisher.publishPaidOff(txn);
        }

        scheduleRepository.saveAll(schedules);
        loanAccountRepository.save(account);

        try {
            transactionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 멱등성 키 고유 제약 위반 — 동일 키가 동시에 처리된 경우 (명세 10번)
            throw new BusinessException(ErrorCode.DUPLICATE_REPAYMENT_REQUEST, idempotencyKey);
        }

        return responseMapper.toRepaymentResponse(txn, account);
    }

    void saveAllocations(RepaymentTransaction txn, List<AllocationLine> lines) {
        List<RepaymentAllocation> rows = new ArrayList<>();
        for (AllocationLine line : lines) {
            rows.add(RepaymentAllocation.builder()
                    .repaymentTransactionId(txn.getId())
                    .repaymentScheduleId(line.scheduleId())
                    .allocationType(line.type())
                    .allocatedAmount(line.amount())
                    .allocationOrder(line.order())
                    .build());
        }
        allocationRepository.saveAll(rows);
    }

    LoanAccount lockAccount(Long loanAccountId) {
        return loanAccountRepository.findByIdForUpdate(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
    }

    List<RepaymentSchedule> loadSchedules(Long loanAccountId) {
        return scheduleRepository.findByLoanAccountIdOrderByDueDateAscInstallmentNumberAscIdAsc(loanAccountId);
    }

    List<ScheduleSnapshot> toSnapshots(List<RepaymentSchedule> schedules) {
        return schedules.stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .map(s -> new ScheduleSnapshot(s.getId(), s.getInstallmentNumber(), s.getDueDate(),
                        s.getOverdueInterest(), s.outstandingInterest(),
                        s.getOverduePrincipal(), s.outstandingPrincipal()))
                .toList();
    }

    /**
     * 동일 멱등성 키 재요청. 요청 내용이 같으면 저장된 결과를 그대로 돌려주고(멱등),
     * 같은 키로 다른 금액·유형이 들어오면 DUPLICATE_REPAYMENT_REQUEST로 거절한다.
     */
    private Optional<RepaymentResponse> replay(String idempotencyKey, RepaymentTransactionType type,
                                                BigDecimal requestedAmount) {
        Optional<RepaymentTransaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        RepaymentTransaction txn = existing.get();
        boolean sameType = txn.getTransactionType() == type;
        boolean sameAmount = requestedAmount == null
                || txn.getRequestedAmount().compareTo(Money.normalize(requestedAmount)) == 0;
        if (!sameType || !sameAmount) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPAYMENT_REQUEST, idempotencyKey);
        }
        LoanAccount account = loanAccountRepository.findById(txn.getLoanAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
        return Optional.of(responseMapper.toRepaymentResponse(txn, account));
    }

    /** 금액을 생략한 자동이체는 도래분 전액을 납부예정금액으로 본다. */
    private BigDecimal resolveAmount(BigDecimal requested, LoanAccount account,
                                      List<RepaymentSchedule> schedules, RepaymentTransactionType type,
                                      LocalDate businessDate) {
        if (requested != null) {
            if (requested.signum() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REPAYMENT_AMOUNT);
            }
            return Money.normalize(requested);
        }
        if (type == RepaymentTransactionType.PREPAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_REPAYMENT_AMOUNT, "중도상환은 금액이 필요합니다");
        }
        BigDecimal due = quoteService.quote(account.getId(), type, businessDate, null).totalAmount();
        if (!Money.isPositive(due)) {
            throw new BusinessException(ErrorCode.INVALID_REPAYMENT_AMOUNT, "도래한 납부예정금액이 없습니다");
        }
        return due;
    }

    /** 중도상환수수료 산정 대상 원금 — 비용·이자를 먼저 배분하므로 원금 몫을 상한으로 추정한다. */
    private BigDecimal prepaymentPrincipalOf(BigDecimal amount, LoanAccount account) {
        return Money.minNonNegative(amount, Money.normalize(account.getPrincipalBalance()));
    }

    private void withdraw(LoanAccount account, BigDecimal amount) {
        if (account.getWithdrawalAccountNumber() == null) {
            throw new BusinessException(ErrorCode.WITHDRAWAL_ACCOUNT_NOT_REGISTERED);
        }
        WithdrawalResult result = withdrawalGateway.withdraw(new WithdrawalRequest(
                account.getWithdrawalAccountNumber(), amount,
                "대출 상환 출금 - " + account.getLoanAccountNumber()));
        if (!result.success()) {
            if ("INSUFFICIENT_BALANCE".equals(result.resultCode())) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_WITHDRAWAL_BALANCE, result.message());
            }
            throw new BusinessException(ErrorCode.WITHDRAWAL_FAILED, result.message());
        }
    }

    private LocalDate resolveBusinessDate(LocalDate paymentDate, LoanAccount account) {
        LocalDate date = paymentDate != null ? paymentDate : LocalDate.now();
        if (date.isBefore(account.getDisbursementDate())) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_DATE,
                    "상환일이 대출 실행일(" + account.getDisbursementDate() + ")보다 빠릅니다");
        }
        return date;
    }

    private RepaymentTransactionType parseType(String value) {
        try {
            return RepaymentTransactionType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "repaymentType=" + value);
        }
    }

    private PaymentMethod parseMethod(String value) {
        try {
            return PaymentMethod.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paymentMethod=" + value);
        }
    }
}
