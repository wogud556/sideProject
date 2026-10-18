package com.hanati.bank.bankEx.loan.mortgage.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.entity.DepositTransaction;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.repository.DepositTransactionRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.mortgage.dto.MortgageDisbursementRequest;
import com.hanati.bank.bankEx.loan.mortgage.dto.MortgageDisbursementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Core/mortgage의 대출 실행금 입금.
 * 멱등 키를 수신 거래번호(TRANSACTION_NUMBER, unique)로 그대로 쓴다.
 * 계좌 잠금을 잡은 뒤 키를 확인하므로, 같은 키의 동시 요청도 한 건만 입금된다.
 */
@Service
@RequiredArgsConstructor
public class mortgageDisbursementService {

    static final String TRANSACTION_TYPE = "MORTGAGE_DISBURSEMENT";

    private final AccountInfoRepository accountInfoRepository;
    private final DepositTransactionRepository depositTransactionRepository;
    private final transService transService;

    @Transactional
    public MortgageDisbursementResponse disburse(MortgageDisbursementRequest request) {
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        AccountInfo account = accountInfoRepository.findByAccountNumberForUpdate(request.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        Optional<DepositTransaction> existing = depositTransactionRepository.findByTransactionNumber(request.idempotencyKey());
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }
        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        DepositTransaction tx = transService.creditLocked(account, request.amount(), TRANSACTION_TYPE,
                request.description(), request.idempotencyKey(), null, LocalDateTime.now());
        return toResponse(tx, false);
    }

    private MortgageDisbursementResponse toResponse(DepositTransaction tx, boolean duplicate) {
        return new MortgageDisbursementResponse(tx.getTransactionNumber(), tx.getAccountNumber(), tx.getAmount(),
                tx.getBalanceAfter(), duplicate);
    }
}
