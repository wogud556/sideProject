package com.hanati.bank.bankEx.deposit.transfer;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseRequest;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.deposit.transfer.entity.AccountTransfer;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import com.hanati.bank.bankEx.deposit.transfer.repository.AccountTransferRepository;
import com.hanati.bank.bankEx.deposit.transfer.service.transferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TransferFailureCasesTests {

    @Autowired
    private loginService loginService;
    @Autowired
    private accountService accountService;
    @Autowired
    private transService transService;
    @Autowired
    private transferService transferService;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private AccountTransferRepository accountTransferRepository;

    private String signup(String userId) {
        SignupResponse response = loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"));
        return response.getAccountNumber();
    }

    private TransferRequest request(String requestId, String userId, String withdrawalAccountNumber,
                                     String depositAccountNumber, long amount) {
        return request(requestId, userId, withdrawalAccountNumber, depositAccountNumber, amount, "1234");
    }

    private TransferRequest request(String requestId, String userId, String withdrawalAccountNumber,
                                     String depositAccountNumber, long amount, String password) {
        return new TransferRequest(requestId, userId, withdrawalAccountNumber, depositAccountNumber,
                amount, password, "생활비", "홍길동");
    }

    @Test
    void withdrawalAccountNotFound_throwsAccountNotFound() {
        String depositUser = "fail1Deposit";
        String depositAccount = signup(depositUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-001", "fail1", "존재하지않는출금계좌", depositAccount, 10_000L)));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
        assertEquals(0L, accountService.getAccount(depositAccount).getBalance());
        assertEquals(0, transService.getTransactions(depositAccount).size());
    }

    @Test
    void depositAccountNotFound_throwsAccountNotFound() {
        String withdrawalUser = "fail2Withdraw";
        String withdrawalAccount = signup(withdrawalUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-002", withdrawalUser, withdrawalAccount, "존재하지않는수취계좌", 10_000L)));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
        // 초기입금(DEPOSIT) 1건만 있고 이체 관련 내역은 추가되지 않아야 한다.
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void withdrawalAccountNotOwnedByRequester_throwsAccountNotOwned() {
        String ownerUser = "fail3Owner";
        String otherUser = "fail3Other";
        String depositUser = "fail3Deposit";
        String ownerAccount = signup(ownerUser);
        signup(otherUser);
        String depositAccount = signup(depositUser);
        transService.deposit(ownerAccount, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-003", otherUser, ownerAccount, depositAccount, 10_000L)));
        assertEquals(ErrorCode.ACCOUNT_NOT_OWNED, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(ownerAccount).getBalance());
        assertEquals(1, transService.getTransactions(ownerAccount).size());
    }

    @Test
    void invalidAccountPassword_throwsInvalidAccountPassword() {
        String withdrawalUser = "fail4Withdraw";
        String depositUser = "fail4Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-004", withdrawalUser, withdrawalAccount, depositAccount, 10_000L, "9999")));
        assertEquals(ErrorCode.INVALID_ACCOUNT_PASSWORD, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void insufficientBalance_throwsInsufficientBalance() {
        String withdrawalUser = "fail5Withdraw";
        String depositUser = "fail5Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(10_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-005", withdrawalUser, withdrawalAccount, depositAccount, 50_000L)));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
        assertEquals(10_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void zeroAmount_throwsInvalidTransferAmount() {
        String withdrawalUser = "fail6Withdraw";
        String depositUser = "fail6Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-006", withdrawalUser, withdrawalAccount, depositAccount, 0L)));
        assertEquals(ErrorCode.INVALID_TRANSFER_AMOUNT, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
    }

    @Test
    void negativeAmount_throwsInvalidTransferAmount() {
        String withdrawalUser = "fail7Withdraw";
        String depositUser = "fail7Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-007", withdrawalUser, withdrawalAccount, depositAccount, -1_000L)));
        assertEquals(ErrorCode.INVALID_TRANSFER_AMOUNT, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
    }

    @Test
    void sameAccountTransfer_throwsSameAccountTransfer() {
        String userId = "fail8";
        String accountNumber = signup(userId);
        transService.deposit(accountNumber, new DepositRequest(50_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-008", userId, accountNumber, accountNumber, 10_000L)));
        assertEquals(ErrorCode.SAME_ACCOUNT_TRANSFER, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(accountNumber).getBalance());
        assertEquals(1, transService.getTransactions(accountNumber).size());
    }

    @Test
    void closedWithdrawalAccount_throwsAccountNotActive() {
        String withdrawalUser = "fail9Withdraw";
        String depositUser = "fail9Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        accountService.closeAccount(withdrawalAccount, new AccountCloseRequest(withdrawalUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-009", withdrawalUser, withdrawalAccount, depositAccount, 10_000L)));
        assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, ex.getErrorCode());
        assertEquals(0, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void suspendedWithdrawalAccount_throwsAccountNotActive() {
        String withdrawalUser = "fail10Withdraw";
        String depositUser = "fail10Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));
        suspendAccount(withdrawalAccount);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-010", withdrawalUser, withdrawalAccount, depositAccount, 10_000L)));
        assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void perTransferLimitExceeded_throwsPerTransferLimitExceeded() {
        String withdrawalUser = "fail11Withdraw";
        String depositUser = "fail11Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(10_000_000L, "초기입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-011", withdrawalUser, withdrawalAccount, depositAccount, 6_000_000L)));
        assertEquals(ErrorCode.PER_TRANSFER_LIMIT_EXCEEDED, ex.getErrorCode());
        assertEquals(10_000_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void dailyTransferLimitExceeded_throwsDailyTransferLimitExceeded() {
        String withdrawalUser = "fail12Withdraw";
        String depositUser = "fail12Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(20_000_000L, "초기입금"));

        transferService.transfer(request("TRF-FAIL-012-A", withdrawalUser, withdrawalAccount, depositAccount, 5_000_000L));
        transferService.transfer(request("TRF-FAIL-012-B", withdrawalUser, withdrawalAccount, depositAccount, 5_000_000L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-012-C", withdrawalUser, withdrawalAccount, depositAccount, 1_000L)));
        assertEquals(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED, ex.getErrorCode());
        assertEquals(10_000_000L, accountService.getAccount(withdrawalAccount).getBalance());
        // 초기입금 1건 + 성공한 이체출금 2건 = 3건. 한도초과로 실패한 3번째 시도는 내역이 추가되면 안 된다.
        assertEquals(3, transService.getTransactions(withdrawalAccount).size());
    }

    @Test
    void nullTransferLimits_fallBackToDefaultLimits() {
        String withdrawalUser = "fail13Withdraw";
        String depositUser = "fail13Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(10_000_000L, "초기입금"));

        AccountInfo account = accountInfoRepository.findByAccountNumber(withdrawalAccount).orElseThrow();
        AccountInfo nullLimitAccount = AccountInfo.builder()
                .accountId(account.getAccountId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .productCode(account.getProductCode())
                .accountName(account.getAccountName())
                .balance(account.getBalance())
                .accountStatus(account.getAccountStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .closedAt(account.getClosedAt())
                .accountPassword(account.getAccountPassword())
                .perTransferLimit(null)
                .dailyTransferLimit(null)
                .build();
        accountInfoRepository.save(nullLimitAccount);

        // 1회 한도(기본값 5,000,000) 초과 -> NPE(500) 대신 PER_TRANSFER_LIMIT_EXCEEDED로 처리되어야 한다.
        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-013-A", withdrawalUser, withdrawalAccount, depositAccount, 6_000_000L)));
        assertEquals(ErrorCode.PER_TRANSFER_LIMIT_EXCEEDED, ex.getErrorCode());

        // 한도 이내 금액은 null 한도값이어도 기본값 기준으로 정상 처리되어야 한다.
        var response = transferService.transfer(request("TRF-FAIL-013-B", withdrawalUser, withdrawalAccount, depositAccount, 1_000_000L));
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(9_000_000L, accountService.getAccount(withdrawalAccount).getBalance());
    }

    @Test
    void duplicateRequestId_withExistingProcessingRecord_throwsDuplicateTransferRequest() {
        String withdrawalUser = "fail14Withdraw";
        String depositUser = "fail14Deposit";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        AccountInfo withdrawal = accountInfoRepository.findByAccountNumber(withdrawalAccount).orElseThrow();
        AccountInfo deposit = accountInfoRepository.findByAccountNumber(depositAccount).orElseThrow();
        accountTransferRepository.save(AccountTransfer.builder()
                .requestId("TRF-FAIL-014")
                .userId(withdrawalUser)
                .withdrawalAccountId(withdrawal.getAccountId())
                .withdrawalAccountNumber(withdrawalAccount)
                .depositAccountId(deposit.getAccountId())
                .depositAccountNumber(depositAccount)
                .amount(10_000L)
                .transferStatus("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transferService.transfer(request("TRF-FAIL-014", withdrawalUser, withdrawalAccount, depositAccount, 10_000L)));
        assertEquals(ErrorCode.DUPLICATE_TRANSFER_REQUEST, ex.getErrorCode());
        assertEquals(50_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(1, transService.getTransactions(withdrawalAccount).size());
    }

    private void suspendAccount(String accountNumber) {
        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber).orElseThrow();
        account.setAccountStatus("SUSPENDED");
        accountInfoRepository.save(account);
    }
}
