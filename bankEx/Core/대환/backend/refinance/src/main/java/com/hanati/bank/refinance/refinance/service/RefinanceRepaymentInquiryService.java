package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.refinance.calculator.RepaymentAmountCalculator;
import com.hanati.bank.refinance.refinance.dto.RepaymentAmountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceRepaymentInquiryService {

    private final ExistingLoanRepository existingLoanRepository;
    private final RepaymentAmountCalculator repaymentAmountCalculator;

    public List<RepaymentAmountResponse> inquire(List<Long> loanIds) {
        return loanIds.stream()
                .map(this::inquireOne)
                .toList();
    }

    private RepaymentAmountResponse inquireOne(Long loanId) {
        ExistingLoan loan = existingLoanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
        return new RepaymentAmountResponse(repaymentAmountCalculator.calculate(loan, LocalDate.now()));
    }
}
