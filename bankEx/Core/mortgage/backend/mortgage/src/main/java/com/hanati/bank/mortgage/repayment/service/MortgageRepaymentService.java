package com.hanati.bank.mortgage.repayment.service;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import com.hanati.bank.mortgage.execution.enums.LoanAccountStatus;
import com.hanati.bank.mortgage.execution.repository.MortgageLoanAccountRepository;
import com.hanati.bank.mortgage.repayment.dto.LoanAccountResponse;
import com.hanati.bank.mortgage.repayment.dto.RepaymentRequest;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentHistory;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;
import com.hanati.bank.mortgage.repayment.enums.ScheduleStatus;
import com.hanati.bank.mortgage.repayment.repository.MortgageRepaymentHistoryRepository;
import com.hanati.bank.mortgage.repayment.repository.MortgageRepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 회차 상환. 다음 미납 회차 1건을 청구액 그대로 납부하는 것만 지원한다.
 * 부분상환·중도상환·연체이자는 범위 밖이다 (Core/repayment 참고).
 */
@Service
@RequiredArgsConstructor
public class MortgageRepaymentService {

    private final MortgageLoanAccountRepository loanAccountRepository;
    private final MortgageRepaymentScheduleRepository scheduleRepository;
    private final MortgageRepaymentHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public LoanAccountResponse getAccount(Long loanAccountId) {
        return toResponse(find(loanAccountId));
    }

    @Transactional(readOnly = true)
    public List<LoanAccountResponse> getCustomerAccounts(String customerId) {
        return loanAccountRepository.findByCustomerIdOrderByLoanAccountIdDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MortgageRepaymentSchedule> getSchedule(Long loanAccountId) {
        find(loanAccountId);
        return scheduleRepository.findByLoanAccountIdOrderByInstallmentNoAsc(loanAccountId);
    }

    @Transactional(readOnly = true)
    public List<MortgageRepaymentHistory> getHistory(Long loanAccountId) {
        find(loanAccountId);
        return historyRepository.findByLoanAccountIdOrderByRepaymentIdDesc(loanAccountId);
    }

    @Transactional
    public MortgageRepaymentHistory repay(Long loanAccountId, RepaymentRequest req) {
        MortgageLoanAccount account = loanAccountRepository.findByIdForUpdate(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
        if (!account.getCustomerId().equals(req.customerId())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_OWNED);
        }
        Optional<MortgageRepaymentHistory> replay = historyRepository.findByIdempotencyKey(req.idempotencyKey());
        if (replay.isPresent()) {
            if (!replay.get().getLoanAccountId().equals(loanAccountId)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            return replay.get();
        }
        if (account.getStatus() == LoanAccountStatus.CLOSED) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_CLOSED);
        }

        MortgageRepaymentSchedule due = nextInstallment(loanAccountId).orElseThrow(
                () -> new BusinessException(ErrorCode.LOAN_ACCOUNT_CLOSED));
        if (!due.getTotalPayment().equals(req.amount())) {
            throw new BusinessException(ErrorCode.REPAYMENT_AMOUNT_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now();
        due.markPaid(now);
        account.repayPrincipal(due.getPrincipal());
        if (nextInstallment(loanAccountId).isEmpty()) {
            account.close();
        }
        return historyRepository.save(MortgageRepaymentHistory.builder()
                .loanAccountId(loanAccountId)
                .installmentNo(due.getInstallmentNo())
                .principalPaid(due.getPrincipal())
                .interestPaid(due.getInterest())
                .totalPaid(due.getTotalPayment())
                .outstandingAfter(account.getOutstandingPrincipal())
                .sourceAccountNo(req.sourceAccountNo())
                .idempotencyKey(req.idempotencyKey())
                .paidAt(now)
                .build());
    }

    private MortgageLoanAccount find(Long loanAccountId) {
        return loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
    }

    private Optional<MortgageRepaymentSchedule> nextInstallment(Long loanAccountId) {
        return scheduleRepository.findFirstByLoanAccountIdAndStatusOrderByInstallmentNoAsc(
                loanAccountId, ScheduleStatus.SCHEDULED);
    }

    private LoanAccountResponse toResponse(MortgageLoanAccount account) {
        return new LoanAccountResponse(account, account.getStatus().getLabel(),
                account.getRepaymentMethod().getLabel(), nextInstallment(account.getLoanAccountId()).orElse(null));
    }
}
