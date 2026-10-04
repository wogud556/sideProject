package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.dto.RepaymentQuoteResponse;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.RepaymentSchedule;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 상환 예정금액 조회 (명세 8.1). 전액상환 금액 계산은 실행 서비스와 공유해
 * 조회값과 처리값이 어긋나지 않게 한다 (명세 7.2).
 */
@Service
@RequiredArgsConstructor
public class RepaymentQuoteService {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    @Transactional(readOnly = true)
    public RepaymentQuoteResponse quote(Long loanAccountId, RepaymentTransactionType repaymentType,
                                         LocalDate paymentDate, BigDecimal requestedPrincipalAmount) {
        LoanAccount account = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
        List<RepaymentSchedule> schedules = repaymentScheduleRepository
                .findByLoanAccountIdOrderByDueDateAscInstallmentNumberAscIdAsc(loanAccountId);

        LocalDate quoteDate = paymentDate != null ? paymentDate : LocalDate.now();
        return build(account, schedules, repaymentType, quoteDate, requestedPrincipalAmount);
    }

    /** 전액상환 예정금액. 실행 시점에 다시 호출해 요청금액과 비교한다. */
    public BigDecimal fullRepaymentTotal(LoanAccount account, List<RepaymentSchedule> schedules,
                                          LocalDate businessDate) {
        return build(account, schedules, RepaymentTransactionType.FULL_REPAYMENT, businessDate, null)
                .totalAmount();
    }

    public BigDecimal prepaymentFee(LoanAccount account, BigDecimal principalToRepay) {
        if (!Money.isPositive(principalToRepay) || !Money.isPositive(account.getPrepaymentFeeRate())) {
            return Money.ZERO;
        }
        return Money.percentOf(principalToRepay, account.getPrepaymentFeeRate());
    }

    private RepaymentQuoteResponse build(LoanAccount account, List<RepaymentSchedule> schedules,
                                          RepaymentTransactionType repaymentType, LocalDate quoteDate,
                                          BigDecimal requestedPrincipalAmount) {
        BigDecimal dueOverdueInterest = Money.ZERO;
        BigDecimal dueInterest = Money.ZERO;
        BigDecimal dueOverduePrincipal = Money.ZERO;
        BigDecimal duePrincipal = Money.ZERO;

        for (RepaymentSchedule s : schedules) {
            if (s.getDueDate().isAfter(quoteDate)) {
                continue;
            }
            dueOverdueInterest = dueOverdueInterest.add(s.getOverdueInterest());
            dueInterest = dueInterest.add(s.outstandingInterest());
            dueOverduePrincipal = dueOverduePrincipal.add(s.getOverduePrincipal());
            duePrincipal = duePrincipal.add(s.outstandingPrincipal());
        }

        BigDecimal principal;
        BigDecimal overduePrincipal;
        BigDecimal prepaymentFee;

        if (repaymentType == RepaymentTransactionType.FULL_REPAYMENT) {
            // 전액상환은 미래 회차 원금까지 모두 회수하되 미래 이자는 받지 않는다 (명세 3.6).
            principal = Money.normalize(account.getPrincipalBalance());
            overduePrincipal = Money.normalize(account.getOverduePrincipal());
            prepaymentFee = prepaymentFee(account, principal);
        } else if (repaymentType == RepaymentTransactionType.PREPAYMENT) {
            BigDecimal requested = Money.normalize(requestedPrincipalAmount);
            principal = Money.minNonNegative(requested, Money.normalize(account.getPrincipalBalance()));
            overduePrincipal = Money.minNonNegative(dueOverduePrincipal,
                    Money.normalize(account.getOverduePrincipal()));
            prepaymentFee = prepaymentFee(account, principal);
        } else {
            principal = Money.minNonNegative(duePrincipal, Money.normalize(account.getPrincipalBalance()));
            overduePrincipal = Money.minNonNegative(dueOverduePrincipal,
                    Money.normalize(account.getOverduePrincipal()));
            prepaymentFee = Money.ZERO;
        }

        BigDecimal fee = Money.normalize(account.getFeeBalance()).add(prepaymentFee);
        BigDecimal total = fee.add(dueOverdueInterest).add(dueInterest).add(overduePrincipal).add(principal);

        return new RepaymentQuoteResponse(
                account.getId(),
                quoteDate,
                repaymentType.name(),
                principal,
                overduePrincipal,
                dueInterest,
                dueOverdueInterest,
                fee,
                total,
                quoteDate.atTime(LocalTime.of(23, 59, 59))
        );
    }
}
