package com.hanati.bank.bankEx.deposit.transfer;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import com.hanati.bank.bankEx.deposit.transfer.service.transferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 진짜 동시성을 재현하기 위해 클래스 레벨 @Transactional을 사용하지 않는다.
 * (테스트 트랜잭션으로 감싸면 각 서비스 호출이 같은 커넥션/트랜잭션에 참여하게 되어 실제 락 경합이 재현되지 않는다.)
 */
@SpringBootTest
class TransferConcurrencyTests {

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
    void concurrentTransfersFromSameAccount_neverGoNegative_andExcessRequestsFailWithInsufficientBalance() throws Exception {
        String withdrawalUser = "concWithdraw1";
        String depositUser = "concDeposit1";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(100_000L, "초기입금"));

        int threadCount = 10;
        long amountPerTransfer = 20_000L;

        List<String> results = runConcurrently(threadCount, i ->
                transferService.transfer(request("TRF-CONC-1-" + i, withdrawalUser, withdrawalAccount, depositAccount, amountPerTransfer)));

        long successCount = results.stream().filter("SUCCESS"::equals).count();
        long insufficientCount = results.stream().filter(ErrorCode.INSUFFICIENT_BALANCE.name()::equals).count();

        assertEquals(threadCount, successCount + insufficientCount);
        assertEquals(5, successCount); // 100,000 / 20,000 = 5건만 성공 가능

        AccountResponse account = accountService.getAccount(withdrawalAccount);
        assertTrue(account.getBalance() >= 0, "잔액이 음수가 되면 안 된다");
        assertEquals(100_000L - amountPerTransfer * successCount, account.getBalance());
    }

    @Test
    void concurrentTransfersWithSameRequestId_onlyOneSucceeds_othersFailWithDuplicateTransferRequest() throws Exception {
        String withdrawalUser = "concWithdraw2";
        String depositUser = "concDeposit2";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(100_000L, "초기입금"));

        String sharedRequestId = "TRF-CONC-2-DUP";
        long amount = 10_000L;

        List<String> results = runConcurrently(2, i ->
                transferService.transfer(request(sharedRequestId, withdrawalUser, withdrawalAccount, depositAccount, amount)));

        long successCount = results.stream().filter("SUCCESS"::equals).count();
        long duplicateCount = results.stream().filter(ErrorCode.DUPLICATE_TRANSFER_REQUEST.name()::equals).count();

        assertEquals(1, successCount);
        assertEquals(1, duplicateCount);

        AccountResponse account = accountService.getAccount(withdrawalAccount);
        assertEquals(100_000L - amount, account.getBalance());
    }

    private List<String> runConcurrently(int threadCount, java.util.function.IntConsumer transferCall) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    transferCall.accept(idx);
                    return "SUCCESS";
                } catch (BusinessException e) {
                    return e.getErrorCode().name();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return results;
    }
}
