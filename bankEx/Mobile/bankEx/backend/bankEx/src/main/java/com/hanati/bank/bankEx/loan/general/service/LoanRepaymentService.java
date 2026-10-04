package com.hanati.bank.bankEx.loan.general.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.general.calculator.LoanInterestCalculator;
import com.hanati.bank.bankEx.loan.general.domain.LoanApplication;
import com.hanati.bank.bankEx.loan.general.domain.LoanProduct;
import com.hanati.bank.bankEx.loan.general.domain.LoanRepaymentHistory;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepayPreviewResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentDetailResponse;
import com.hanati.bank.bankEx.loan.general.enums.LoanRepaymentType;
import com.hanati.bank.bankEx.loan.general.mapper.LoanApplicationMapper;
import com.hanati.bank.bankEx.loan.general.mapper.LoanProductMapper;
import com.hanati.bank.bankEx.loan.general.mapper.LoanRepaymentHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRepaymentService {

    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanProductMapper loanProductMapper;
    private final LoanRepaymentHistoryMapper historyMapper;
    private final transService transService;

    @Transactional
    public LoanRepaymentDetailResponse repay(Long loanId) {
        LoanApplication application = loanApplicationMapper.findById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_APPLICATION_NOT_FOUND));

        if (!"실행완료".equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_NOT_APPROVED);
        }

        Long remaining = application.getRemainingBalance();
        if (remaining == null || remaining <= 0) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_COMPLETED);
        }

        LoanProduct product = loanProductMapper.findById(application.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_PRODUCT_NOT_FOUND));

        if (product.getInterestRate() == null || product.getInterestRate() <= 0) {
            throw new BusinessException(ErrorCode.LOAN_INTEREST_RATE_INVALID);
        }

        BigDecimal remainingBD = BigDecimal.valueOf(remaining);
        BigDecimal annualRate = BigDecimal.valueOf(product.getInterestRate());

        BigDecimal interestBD = LoanInterestCalculator.calculateMonthlyInterest(remainingBD, annualRate);

        LoanRepaymentType repaymentType = resolveType(application.getRepaymentType());
        int paymentSeq = historyMapper.countByApplicationId(loanId) + 1;
        boolean isLastPayment = (paymentSeq >= application.getLoanPeriod());

        BigDecimal principalBD;
        if (repaymentType == LoanRepaymentType.BULLET) {
            principalBD = isLastPayment ? remainingBD : BigDecimal.ZERO;
        } else {
            // EQUAL_PRINCIPAL: 마지막 회차이거나 잔액이 고정원금보다 적으면 잔액 전액
            BigDecimal fixedPrincipal = LoanInterestCalculator.calculateEqualPrincipalAmount(
                    BigDecimal.valueOf(application.getRequestAmount()), application.getLoanPeriod());
            principalBD = isLastPayment ? remainingBD : fixedPrincipal.min(remainingBD);
        }

        BigDecimal totalBD = principalBD.add(interestBD);
        long principalAmount = principalBD.longValue();
        long interestAmount = interestBD.longValue();
        long totalPayment = totalBD.longValue();

        transService.debit(application.getAccountNumber(), totalPayment, "LOAN_REPAYMENT", "대출 상환 출금");

        long newRemaining = remaining - principalAmount;
        application.setRemainingBalance(newRemaining);

        String newStatus = newRemaining <= 0 ? "상환완료" : application.getStatus();
        application.setStatus(newStatus);
        loanApplicationMapper.update(application);

        LoanRepaymentHistory history = LoanRepaymentHistory.builder()
                .applicationId(loanId)
                .paymentSeq(paymentSeq)
                .repaymentType(repaymentType.name())
                .totalPayment(totalPayment)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(Math.max(newRemaining, 0))
                .paidAt(LocalDateTime.now())
                .build();
        historyMapper.insert(history);

        return new LoanRepaymentDetailResponse(
                loanId, totalPayment, principalAmount, interestAmount,
                Math.max(newRemaining, 0), newStatus);
    }

    @Transactional(readOnly = true)
    public LoanRepayPreviewResponse preview(Long loanId) {
        LoanApplication application = loanApplicationMapper.findById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_APPLICATION_NOT_FOUND));

        Long remaining = application.getRemainingBalance();
        if (remaining == null || remaining <= 0) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_COMPLETED);
        }

        LoanProduct product = loanProductMapper.findById(application.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_PRODUCT_NOT_FOUND));

        if (product.getInterestRate() == null || product.getInterestRate() <= 0) {
            throw new BusinessException(ErrorCode.LOAN_INTEREST_RATE_INVALID);
        }

        BigDecimal remainingBD = BigDecimal.valueOf(remaining);
        BigDecimal annualRate = BigDecimal.valueOf(product.getInterestRate());
        BigDecimal interestBD = LoanInterestCalculator.calculateMonthlyInterest(remainingBD, annualRate);

        LoanRepaymentType repaymentType = resolveType(application.getRepaymentType());
        int paymentSeq = historyMapper.countByApplicationId(loanId) + 1;
        boolean isLastPayment = (paymentSeq >= application.getLoanPeriod());

        BigDecimal principalBD;
        if (repaymentType == LoanRepaymentType.BULLET) {
            principalBD = isLastPayment ? remainingBD : BigDecimal.ZERO;
        } else {
            BigDecimal fixedPrincipal = LoanInterestCalculator.calculateEqualPrincipalAmount(
                    BigDecimal.valueOf(application.getRequestAmount()), application.getLoanPeriod());
            principalBD = isLastPayment ? remainingBD : fixedPrincipal.min(remainingBD);
        }

        return new LoanRepayPreviewResponse(
                loanId, remaining,
                principalBD.longValue(),
                interestBD.longValue(),
                principalBD.add(interestBD).longValue(),
                repaymentType.name());
    }

    public List<LoanRepaymentHistory> getHistory(Long loanId) {
        loanApplicationMapper.findById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_APPLICATION_NOT_FOUND));
        return historyMapper.findByApplicationId(loanId);
    }

    private LoanRepaymentType resolveType(String type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.LOAN_INVALID_REPAYMENT_TYPE);
        }
        try {
            return LoanRepaymentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.LOAN_INVALID_REPAYMENT_TYPE);
        }
    }
}
