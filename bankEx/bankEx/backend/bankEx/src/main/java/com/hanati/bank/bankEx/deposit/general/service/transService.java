package com.hanati.bank.bankEx.deposit.general.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.dto.TransactionResponse;
import com.hanati.bank.bankEx.deposit.general.dto.WithdrawRequest;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.entity.DepositTransaction;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.repository.DepositTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class transService {

    private final AccountInfoRepository accountInfoRepository;
    private final DepositTransactionRepository depositTransactionRepository;

    @Transactional
    public TransactionResponse deposit(String accountNumber, DepositRequest request) {
        return credit(accountNumber, request.getAmount(), "DEPOSIT", request.getDescription());
    }

    @Transactional
    public TransactionResponse withdraw(String accountNumber, WithdrawRequest request) {
        return debit(accountNumber, request.getAmount(), "WITHDRAW", request.getDescription());
    }

    public List<TransactionResponse> getTransactions(String accountNumber) {
        return depositTransactionRepository.findByAccountNumberOrderByTransactionAtDesc(accountNumber).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse credit(String accountNumber, Long amount, String transactionType, String description) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        AccountInfo account = getActiveAccount(accountNumber);

        LocalDateTime now = LocalDateTime.now();
        account.setBalance(account.getBalance() + amount);
        account.setUpdatedAt(now);
        accountInfoRepository.save(account);

        DepositTransaction transaction = recordTransaction(account, transactionType, amount, description, now);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse debit(String accountNumber, Long amount, String transactionType, String description) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        AccountInfo account = getActiveAccount(accountNumber);

        if (account.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        LocalDateTime now = LocalDateTime.now();
        account.setBalance(account.getBalance() - amount);
        account.setUpdatedAt(now);
        accountInfoRepository.save(account);

        DepositTransaction transaction = recordTransaction(account, transactionType, amount, description, now);
        return toResponse(transaction);
    }

    private AccountInfo getActiveAccount(String accountNumber) {
        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    private DepositTransaction recordTransaction(AccountInfo account, String transactionType, Long amount,
                                                  String description, LocalDateTime transactionAt) {
        DepositTransaction transaction = DepositTransaction.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .description(description)
                .transactionAt(transactionAt)
                .build();
        return depositTransactionRepository.save(transaction);
    }

    private TransactionResponse toResponse(DepositTransaction transaction) {
        return new TransactionResponse(
                transaction.getAccountNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getTransactionAt().toString()
        );
    }
}
