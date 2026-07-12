package com.hanati.bank.bankEx.deposit.transfer.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.NameMaskUtil;
import com.hanati.bank.bankEx.common.util.TransactionNoGenerator;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.repository.DepositTransactionRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import com.hanati.bank.bankEx.deposit.transfer.dto.AccountHolderResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResultResponse;
import com.hanati.bank.bankEx.deposit.transfer.entity.AccountTransfer;
import com.hanati.bank.bankEx.deposit.transfer.repository.AccountTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class transferService {

    private final AccountInfoRepository accountInfoRepository;
    private final DepositTransactionRepository depositTransactionRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final UserInfoRepository userInfoRepository;
    private final transService transService;

    public AccountHolderResponse getHolder(String accountNumber) {
        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        String holderName = resolveHolderName(account.getUserId());
        return new AccountHolderResponse(account.getAccountNumber(), NameMaskUtil.mask(holderName), account.getAccountStatus());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (isBlank(request.getRequestId()) || isBlank(request.getUserId())
                || isBlank(request.getWithdrawalAccountNumber()) || isBlank(request.getDepositAccountNumber())
                || isBlank(request.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_AMOUNT);
        }

        Optional<AccountTransfer> existing = accountTransferRepository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            if ("COMPLETED".equals(existing.get().getTransferStatus())) {
                return toResponse(existing.get());
            }
            throw new BusinessException(ErrorCode.DUPLICATE_TRANSFER_REQUEST);
        }

        String first = request.getWithdrawalAccountNumber().compareTo(request.getDepositAccountNumber()) <= 0
                ? request.getWithdrawalAccountNumber() : request.getDepositAccountNumber();
        String second = first.equals(request.getWithdrawalAccountNumber())
                ? request.getDepositAccountNumber() : request.getWithdrawalAccountNumber();
        AccountInfo firstLocked = accountInfoRepository.findByAccountNumberForUpdate(first)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountInfo secondLocked = first.equals(second) ? firstLocked
                : accountInfoRepository.findByAccountNumberForUpdate(second)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountInfo withdrawal = request.getWithdrawalAccountNumber().equals(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;
        AccountInfo deposit = request.getDepositAccountNumber().equals(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;

        if (!withdrawal.getUserId().equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OWNED);
        }

        if (!"ACTIVE".equals(withdrawal.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (!"ACTIVE".equals(deposit.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        if (withdrawal.getAccountNumber().equals(deposit.getAccountNumber())) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        if (!request.getAccountPassword().equals(withdrawal.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }

        if (withdrawal.getBalance() < request.getAmount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        long perLimit = withdrawal.getPerTransferLimit() != null ? withdrawal.getPerTransferLimit() : 5_000_000L;
        if (request.getAmount() > perLimit) {
            throw new BusinessException(ErrorCode.PER_TRANSFER_LIMIT_EXCEEDED);
        }
        long dailyLimit = withdrawal.getDailyTransferLimit() != null ? withdrawal.getDailyTransferLimit() : 10_000_000L;
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        Long todaySum = depositTransactionRepository.sumTransferWithdrawalAmount(
                withdrawal.getAccountNumber(), startOfDay, startOfNextDay);
        if (todaySum + request.getAmount() > dailyLimit) {
            throw new BusinessException(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED);
        }

        LocalDateTime now = LocalDateTime.now();
        AccountTransfer transfer;
        try {
            transfer = accountTransferRepository.save(AccountTransfer.builder()
                    .requestId(request.getRequestId())
                    .userId(request.getUserId())
                    .withdrawalAccountId(withdrawal.getAccountId())
                    .withdrawalAccountNumber(withdrawal.getAccountNumber())
                    .depositAccountId(deposit.getAccountId())
                    .depositAccountNumber(deposit.getAccountNumber())
                    .amount(request.getAmount())
                    .withdrawalMemo(request.getWithdrawalMemo())
                    .depositMemo(request.getDepositMemo())
                    .transferStatus("PROCESSING")
                    .createdAt(now)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_TRANSFER_REQUEST);
        }

        String withdrawalTxnNo = TransactionNoGenerator.generate(depositTransactionRepository::existsByTransactionNumber);
        var withdrawalTx = transService.debitLocked(withdrawal, request.getAmount(), "TRANSFER_WITHDRAWAL",
                request.getWithdrawalMemo(), withdrawalTxnNo, transfer.getTransferId(), now);

        String depositTxnNo = TransactionNoGenerator.generate(depositTransactionRepository::existsByTransactionNumber);
        transService.creditLocked(deposit, request.getAmount(), "TRANSFER_DEPOSIT",
                request.getDepositMemo(), depositTxnNo, transfer.getTransferId(), now);

        transfer.setTransferStatus("COMPLETED");
        transfer.setTransferredAt(now);
        transfer.setTransactionNumber(withdrawalTx.getTransactionNumber());
        transfer.setBalanceAfterTransfer(withdrawal.getBalance());
        accountTransferRepository.save(transfer);

        return toResponse(transfer);
    }

    public TransferResultResponse getTransfer(Long transferId) {
        AccountTransfer transfer = accountTransferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        String depositHolderName = accountInfoRepository.findByAccountNumber(transfer.getDepositAccountNumber())
                .map(a -> resolveHolderName(a.getUserId()))
                .orElse(null);

        return new TransferResultResponse(
                transfer.getTransferId(),
                transfer.getTransactionNumber(),
                transfer.getWithdrawalAccountNumber(),
                transfer.getDepositAccountNumber(),
                NameMaskUtil.mask(depositHolderName),
                transfer.getAmount(),
                transfer.getBalanceAfterTransfer(),
                transfer.getTransferStatus(),
                transfer.getTransferredAt() == null ? null : transfer.getTransferredAt().toString()
        );
    }

    private String resolveHolderName(String userId) {
        return userInfoRepository.findById(userId)
                .map(UserInfo::getUserName)
                .orElse(null);
    }

    private TransferResponse toResponse(AccountTransfer transfer) {
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getTransactionNumber(),
                transfer.getTransferStatus(),
                transfer.getAmount(),
                transfer.getBalanceAfterTransfer(),
                transfer.getTransferredAt() == null ? null : transfer.getTransferredAt().toString()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
