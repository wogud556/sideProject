package com.hanati.bank.bankEx.deposit.transfer;

import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.dto.TransactionResponse;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.deposit.transfer.dto.AccountHolderResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResponse;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferResultResponse;
import com.hanati.bank.bankEx.deposit.transfer.service.transferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class TransferFlowTests {

    @Autowired
    private loginService loginService;
    @Autowired
    private accountService accountService;
    @Autowired
    private transService transService;
    @Autowired
    private transferService transferService;

    private String signup(String userId) {
        SignupResponse response = loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"));
        return response.getAccountNumber();
    }

    private TransferRequest request(String requestId, String userId, String withdrawalAccountNumber,
                                     String depositAccountNumber, long amount) {
        return new TransferRequest(requestId, userId, withdrawalAccountNumber, depositAccountNumber,
                amount, "1234", "생활비", "홍길동");
    }

    @Test
    void transfer_success_updatesBalancesAndCreatesTransactionsAndLedger() {
        String withdrawalUser = "trWithdraw1";
        String depositUser = "trDeposit1";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(1_000_000L, "초기입금"));

        TransferResponse response = transferService.transfer(
                request("TRF-FLOW-001", withdrawalUser, withdrawalAccount, depositAccount, 50_000L));

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(50_000L, response.getAmount());
        assertEquals(950_000L, response.getBalanceAfterTransfer());
        assertNotNull(response.getTransactionNumber());
        assertNotNull(response.getTransferId());

        assertEquals(950_000L, accountService.getAccount(withdrawalAccount).getBalance());
        assertEquals(50_000L, accountService.getAccount(depositAccount).getBalance());

        // 초기입금(DEPOSIT) 1건 + 이체출금(TRANSFER_WITHDRAWAL) 1건 = 총 2건 (최신순 정렬이라 0번이 이체출금)
        List<TransactionResponse> withdrawalHistory = transService.getTransactions(withdrawalAccount);
        assertEquals(2, withdrawalHistory.size());
        assertEquals("TRANSFER_WITHDRAWAL", withdrawalHistory.get(0).getTransactionType());
        assertEquals(50_000L, withdrawalHistory.get(0).getAmount());

        List<TransactionResponse> depositHistory = transService.getTransactions(depositAccount);
        assertEquals(1, depositHistory.size());
        assertEquals("TRANSFER_DEPOSIT", depositHistory.get(0).getTransactionType());
        assertEquals(50_000L, depositHistory.get(0).getAmount());

        TransferResultResponse result = transferService.getTransfer(response.getTransferId());
        assertEquals("COMPLETED", result.getTransferStatus());
        assertEquals(response.getTransactionNumber(), result.getTransactionNumber());
    }

    @Test
    void getHolder_returnsMaskedAccountHolderName() {
        String userId = "trHolder1";
        String accountNumber = signup(userId);

        AccountHolderResponse holder = transferService.getHolder(accountNumber);

        assertEquals(accountNumber, holder.getAccountNumber());
        assertEquals("홍*동", holder.getAccountHolderName());
        assertEquals("ACTIVE", holder.getAccountStatus());
    }

    @Test
    void getTransfer_returnsTransferResultWithMaskedDepositHolderName() {
        String withdrawalUser = "trWithdraw2";
        String depositUser = "trDeposit2";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(200_000L, "초기입금"));

        TransferResponse response = transferService.transfer(
                request("TRF-FLOW-002", withdrawalUser, withdrawalAccount, depositAccount, 30_000L));

        TransferResultResponse result = transferService.getTransfer(response.getTransferId());

        assertEquals(withdrawalAccount, result.getWithdrawalAccountNumber());
        assertEquals(depositAccount, result.getDepositAccountNumber());
        assertEquals("홍*동", result.getDepositAccountHolderName());
        assertEquals(30_000L, result.getAmount());
        assertEquals(170_000L, result.getBalanceAfterTransfer());
    }

    @Test
    void duplicateRequestId_afterCompleted_returnsIdempotentResponseWithoutDoubleWithdrawal() {
        String withdrawalUser = "trWithdraw3";
        String depositUser = "trDeposit3";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(100_000L, "초기입금"));

        TransferRequest req = request("TRF-FLOW-003", withdrawalUser, withdrawalAccount, depositAccount, 40_000L);

        TransferResponse first = transferService.transfer(req);
        TransferResponse second = transferService.transfer(req);

        assertEquals(first.getTransferId(), second.getTransferId());
        assertEquals(first.getTransactionNumber(), second.getTransactionNumber());
        assertEquals(first.getBalanceAfterTransfer(), second.getBalanceAfterTransfer());

        AccountResponse withdrawal = accountService.getAccount(withdrawalAccount);
        assertEquals(60_000L, withdrawal.getBalance());
        // 초기입금(DEPOSIT) 1건 + 이체출금(TRANSFER_WITHDRAWAL) 1건. 재요청으로 추가 출금내역이 생기면 안 된다.
        assertEquals(2, transService.getTransactions(withdrawalAccount).size());
        assertEquals(1, transService.getTransactions(depositAccount).size());
    }
}
