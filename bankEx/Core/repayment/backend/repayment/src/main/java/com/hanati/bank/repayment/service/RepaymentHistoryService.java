package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.dto.RepaymentTransactionResponse;
import com.hanati.bank.repayment.entity.RepaymentTransaction;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.RepaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 상환 거래내역 조회 (명세 8.5). */
@Service
@RequiredArgsConstructor
public class RepaymentHistoryService {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentTransactionRepository transactionRepository;
    private final RepaymentResponseMapper responseMapper;

    @Transactional(readOnly = true)
    public List<RepaymentTransactionResponse> getTransactions(Long loanAccountId) {
        requireAccount(loanAccountId);
        return transactionRepository.findByLoanAccountIdOrderByIdDesc(loanAccountId).stream()
                .map(responseMapper::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepaymentTransactionResponse getTransaction(Long loanAccountId, String transactionNumber) {
        requireAccount(loanAccountId);
        RepaymentTransaction txn = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAYMENT_TRANSACTION_NOT_FOUND));
        if (!txn.getLoanAccountId().equals(loanAccountId)) {
            throw new BusinessException(ErrorCode.REPAYMENT_TRANSACTION_NOT_FOUND,
                    "거래가 대출계좌 " + loanAccountId + "에 속하지 않습니다");
        }
        return responseMapper.toTransactionResponse(txn);
    }

    private void requireAccount(Long loanAccountId) {
        if (!loanAccountRepository.existsById(loanAccountId)) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND);
        }
    }
}
