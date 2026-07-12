package com.hanati.bank.bankEx.deposit.general.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.AccountNoGenerator;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseResponse;
import com.hanati.bank.bankEx.deposit.general.dto.AccountOpenRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.loan.general.service.loanService;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class accountService {

    private final AccountInfoRepository accountInfoRepository;
    private final UserInfoRepository userInfoRepository;
    private final loanService loanService;

    public List<AccountResponse> getAccounts(String userId) {
        return accountInfoRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(String accountNumber) {
        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse openAccount(AccountOpenRequest request) {
        UserInfo user = userInfoRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        if (!"ACTIVE".equals(user.getCustomerStatus())) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_ACTIVE);
        }

        String accountNumber = AccountNoGenerator.generate(accountInfoRepository::existsByAccountNumber);
        LocalDateTime now = LocalDateTime.now();
        AccountInfo account = AccountInfo.builder()
                .userId(request.getUserId())
                .accountNumber(accountNumber)
                .productCode(request.getProductCode())
                .accountName(request.getAccountName())
                .balance(0L)
                .accountStatus("ACTIVE")
                .createdAt(now)
                .build();
        accountInfoRepository.save(account);

        return toResponse(account);
    }

    @Transactional
    public AccountCloseResponse closeAccount(String accountNumber, AccountCloseRequest request) {
        AccountInfo account = accountInfoRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUserId().equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if ("CLOSED".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_CLOSED);
        }

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        if (account.getBalance() != 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO);
        }

        if (loanService.hasActiveLoan(account.getUserId(), accountNumber)) {
            throw new BusinessException(ErrorCode.ACCOUNT_HAS_ACTIVE_LOAN);
        }

        LocalDateTime now = LocalDateTime.now();
        account.setAccountStatus("CLOSED");
        account.setClosedAt(now);
        account.setUpdatedAt(now);
        accountInfoRepository.save(account);

        return new AccountCloseResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountStatus(),
                account.getClosedAt().toString(),
                "계좌 해지가 완료되었습니다."
        );
    }

    private AccountResponse toResponse(AccountInfo account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCreatedAt().toString(),
                account.getAccountStatus()
        );
    }
}
