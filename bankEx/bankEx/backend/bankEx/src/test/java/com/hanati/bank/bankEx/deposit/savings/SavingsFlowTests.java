package com.hanati.bank.bankEx.deposit.savings;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsProduct;
import com.hanati.bank.bankEx.deposit.savings.dto.AutoTransferBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.MaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsAccountResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsCancelResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeRequest;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeResponse;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsProductStatus;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsProductMapper;
import com.hanati.bank.bankEx.deposit.savings.service.autoTransferService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsAccountService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsCancelService;
import com.hanati.bank.bankEx.deposit.savings.service.savingsMaturityService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.service.loginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class SavingsFlowTests {

    private static final String PRODUCT_ID = "SAV_BASIC_001";

    @Autowired
    private loginService loginService;
    @Autowired
    private transService transService;
    @Autowired
    private accountService accountService;
    @Autowired
    private savingsAccountService savingsAccountService;
    @Autowired
    private savingsCancelService savingsCancelService;
    @Autowired
    private autoTransferService autoTransferService;
    @Autowired
    private savingsMaturityService savingsMaturityService;
    @Autowired
    private SavingsProductMapper savingsProductMapper;

    private String signupAndFundAccount(String userId, long depositAmount) {
        String accountNumber = loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678"))
                .getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(depositAmount, "테스트 입금"));
        return accountNumber;
    }

    @Test
    void subscribeWithAutoTransfer_payTwelveTimes_maturesAndPaysOut() {
        String checkingAccount = signupAndFundAccount("savUser1", 7_000_000L);

        SavingsSubscribeRequest request = new SavingsSubscribeRequest(
                "savUser1", checkingAccount, PRODUCT_ID, 500_000L, 12, 5,
                true, false, false);
        SavingsSubscribeResponse subscribeResponse = savingsAccountService.subscribe(request);
        assertEquals("ACTIVE", subscribeResponse.getStatus());
        assertEquals(2.80 + 0.3 + 0.3, subscribeResponse.getInterestRate(), 0.001);

        String accountNo = subscribeResponse.getAccountNo();

        LocalDate transferDate = LocalDate.of(2026, 7, 5);
        for (int i = 0; i < 12; i++) {
            AutoTransferBatchResponse batch = autoTransferService.execute(transferDate.plusMonths(i));
            assertEquals(1, batch.getSuccessCount());
            assertEquals(0, batch.getFailedCount());
        }

        SavingsAccountResponse afterPayments = savingsAccountService.getAccount(accountNo);
        assertEquals(12, afterPayments.getCurrentCount());
        assertEquals(6_000_000L, afterPayments.getBalance());
        assertEquals("ACTIVE", afterPayments.getStatus());

        long checkingBalanceBeforeMaturity = accountService.getAccount(checkingAccount).getBalance();

        MaturityBatchResponse maturityBatch = savingsMaturityService.execute(LocalDate.now().plusYears(2));
        assertEquals(1, maturityBatch.getProcessedCount());

        SavingsAccountResponse afterMaturity = savingsAccountService.getAccount(accountNo);
        assertEquals("TERMINATED", afterMaturity.getStatus());

        long checkingBalanceAfterMaturity = accountService.getAccount(checkingAccount).getBalance();
        assertTrue(checkingBalanceAfterMaturity > checkingBalanceBeforeMaturity,
                "만기 원리금이 출금계좌로 입금되어 잔액이 늘어나야 한다");
    }

    @Test
    void manualPaymentThenCancel_paysOutReducedInterest() {
        String checkingAccount = signupAndFundAccount("savUser2", 2_000_000L);

        SavingsSubscribeRequest request = new SavingsSubscribeRequest(
                "savUser2", checkingAccount, PRODUCT_ID, 300_000L, 12, null,
                false, false, false);
        SavingsSubscribeResponse subscribeResponse = savingsAccountService.subscribe(request);
        assertEquals("OPEN", subscribeResponse.getStatus());

        String accountNo = subscribeResponse.getAccountNo();
        savingsAccountService.pay(accountNo, null);
        savingsAccountService.pay(accountNo, null);

        SavingsAccountResponse afterPayments = savingsAccountService.getAccount(accountNo);
        assertEquals(2, afterPayments.getCurrentCount());
        assertEquals("ACTIVE", afterPayments.getStatus());

        SavingsCancelResponse cancelResponse = savingsCancelService.cancel(accountNo);
        assertEquals("CANCELLED", cancelResponse.getStatus());
        assertTrue(cancelResponse.getPayoutAmount() > 0);

        assertEquals("CANCELLED", savingsAccountService.getAccount(accountNo).getStatus());
    }

    @Test
    void cancelAlreadyCancelledAccount_throws() {
        String checkingAccount = signupAndFundAccount("savUser3", 1_000_000L);
        SavingsSubscribeResponse subscribeResponse = savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser3", checkingAccount, PRODUCT_ID, 200_000L, 12, null, false, false, false));

        savingsCancelService.cancel(subscribeResponse.getAccountNo());

        assertThrows(BusinessException.class, () -> savingsCancelService.cancel(subscribeResponse.getAccountNo()));
    }

    @Test
    void insufficientWithdrawBalance_isRejected() {
        String checkingAccount = signupAndFundAccount("savUser4", 100_000L);

        assertThrows(BusinessException.class, () -> savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser4", checkingAccount, PRODUCT_ID, 500_000L, 12, null, false, false, false)));
    }

    @Test
    void closedProduct_isRejected() {
        savingsProductMapper.insert(SavingsProduct.builder()
                .productId("SAV_CLOSED_001")
                .productName("판매종료 상품")
                .baseRate(2.5)
                .maxRate(3.0)
                .minAmount(100_000L)
                .maxAmount(1_000_000L)
                .period(12)
                .autoTransferYn("Y")
                .status(SavingsProductStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build());

        String checkingAccount = signupAndFundAccount("savUser5", 1_000_000L);

        assertThrows(BusinessException.class, () -> savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser5", checkingAccount, "SAV_CLOSED_001", 200_000L, 12, null, false, false, false)));
    }

    @Test
    void amountAboveProductMax_isRejected() {
        String checkingAccount = signupAndFundAccount("savUser6", 10_000_000L);

        assertThrows(BusinessException.class, () -> savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser6", checkingAccount, PRODUCT_ID, 5_000_000L, 12, null, false, false, false)));
    }

    @Test
    void duplicatedSubscription_isRejected() {
        String checkingAccount = signupAndFundAccount("savUser7", 2_000_000L);
        savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser7", checkingAccount, PRODUCT_ID, 200_000L, 12, null, false, false, false));

        assertThrows(BusinessException.class, () -> savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser7", checkingAccount, PRODUCT_ID, 200_000L, 12, null, false, false, false)));
    }

    @Test
    void invalidTransferDay_isRejected() {
        String checkingAccount = signupAndFundAccount("savUser8", 2_000_000L);

        assertThrows(BusinessException.class, () -> savingsAccountService.subscribe(new SavingsSubscribeRequest(
                "savUser8", checkingAccount, PRODUCT_ID, 200_000L, 12, 7, false, false, false)));
    }
}
