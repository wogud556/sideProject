package com.hanati.bank.bankEx.deposit.fixed;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositAccountResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityBatchResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityPreviewResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositProductResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeRequest;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositTerminateResponse;
import com.hanati.bank.bankEx.deposit.fixed.service.FixedDepositMaturityService;
import com.hanati.bank.bankEx.deposit.fixed.service.FixedDepositService;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.service.loginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class FixedDepositFlowTests {

    private static final String PRODUCT_ID = "FD_BASIC_001";
    private static final long PRINCIPAL = 10_000_000L;

    @Autowired
    private loginService loginService;
    @Autowired
    private transService transService;
    @Autowired
    private accountService accountService;
    @Autowired
    private FixedDepositService fixedDepositService;
    @Autowired
    private FixedDepositMaturityService fixedDepositMaturityService;

    private String signupAndFundAccount(String userId, long depositAmount) {
        String accountNumber = loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"))
                .getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(depositAmount, "테스트 입금"));
        return accountNumber;
    }

    private FixedDepositSubscribeResponse subscribe(String userId, String withdrawAccountNo, long principal) {
        return fixedDepositService.subscribe(
                new FixedDepositSubscribeRequest(userId, PRODUCT_ID, withdrawAccountNo, principal, 12));
    }

    // ---------- 상품 조회 ----------

    @Test
    void getProducts_returnsSeededOnSaleProduct() {
        List<FixedDepositProductResponse> products = fixedDepositService.getProducts();

        assertTrue(products.stream().anyMatch(p -> PRODUCT_ID.equals(p.getDepositProductId())));
        FixedDepositProductResponse product = products.stream()
                .filter(p -> PRODUCT_ID.equals(p.getDepositProductId()))
                .findFirst().orElseThrow();
        assertEquals("정기예금", product.getProductName());
        assertEquals(12, product.getTermMonths());
        assertEquals(3.50, product.getInterestRate(), 0.001);
        assertEquals(0.50, product.getEarlyTerminationRate(), 0.001);
        assertEquals("ON_SALE", product.getStatus());
    }

    // ---------- 가입 ----------

    @Test
    void subscribe_withdrawsPrincipalAndStoresExpectedMaturityAmount() {
        String checkingAccount = signupAndFundAccount("fdUser1", 20_000_000L);

        FixedDepositSubscribeResponse response = subscribe("fdUser1", checkingAccount, PRINCIPAL);

        // 만기이자 = 10,000,000 * 3.5% * 12/12 = 350,000 / 세금 = 53,900 / 지급예상 = 10,296,100
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(PRINCIPAL, response.getPrincipalAmount());
        assertEquals(350_000L, response.getExpectedInterest());
        assertEquals(10_296_100L, response.getExpectedMaturityAmount());
        assertEquals(LocalDate.now(), response.getStartDate());
        assertEquals(LocalDate.now().plusMonths(12), response.getMaturityDate());

        // 출금계좌에서 원금이 빠져나갔는지 확인
        assertEquals(20_000_000L - PRINCIPAL, accountService.getAccount(checkingAccount).getBalance());
    }

    @Test
    void getAccount_returnsSubscribedAccountWithProductName() {
        String checkingAccount = signupAndFundAccount("fdUser2", 20_000_000L);
        String depositAccountId = subscribe("fdUser2", checkingAccount, PRINCIPAL).getDepositAccountId();

        FixedDepositAccountResponse account = fixedDepositService.getAccount(depositAccountId);

        assertEquals(depositAccountId, account.getDepositAccountId());
        assertEquals("정기예금", account.getProductName());
        assertEquals(PRINCIPAL, account.getPrincipalAmount());
        assertEquals(3.50, account.getInterestRate(), 0.001);
        assertEquals("ACTIVE", account.getStatus());
    }

    @Test
    void maturityPreview_breaksDownInterestAndTax() {
        String checkingAccount = signupAndFundAccount("fdUser3", 20_000_000L);
        String depositAccountId = subscribe("fdUser3", checkingAccount, PRINCIPAL).getDepositAccountId();

        FixedDepositMaturityPreviewResponse preview = fixedDepositService.getMaturityPreview(depositAccountId);

        assertEquals(PRINCIPAL, preview.getPrincipalAmount());
        assertEquals(350_000L, preview.getExpectedInterest());
        assertEquals(53_900L, preview.getTaxAmount());
        assertEquals(10_296_100L, preview.getExpectedMaturityAmount());
    }

    // ---------- 만기 ----------

    @Test
    void maturityBatch_paysPrincipalAndInterestAndMarksMatured() {
        String checkingAccount = signupAndFundAccount("fdUser4", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser4", checkingAccount, PRINCIPAL);
        String depositAccountId = subscribed.getDepositAccountId();
        long balanceAfterSubscribe = accountService.getAccount(checkingAccount).getBalance();

        FixedDepositMaturityBatchResponse batch = fixedDepositMaturityService.execute(subscribed.getMaturityDate());

        assertEquals(1, batch.getProcessedCount());
        assertEquals("MATURED", fixedDepositService.getAccount(depositAccountId).getStatus());
        assertEquals(balanceAfterSubscribe + 10_296_100L,
                accountService.getAccount(checkingAccount).getBalance());
    }

    @Test
    void maturityBatch_skipsAccountsBeforeMaturityDate() {
        String checkingAccount = signupAndFundAccount("fdUser5", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser5", checkingAccount, PRINCIPAL);

        FixedDepositMaturityBatchResponse batch =
                fixedDepositMaturityService.execute(subscribed.getMaturityDate().minusDays(1));

        assertEquals(0, batch.getProcessedCount());
        assertEquals("ACTIVE", fixedDepositService.getAccount(subscribed.getDepositAccountId()).getStatus());
    }

    // ---------- 중도해지 ----------

    @Test
    void terminate_appliesEarlyTerminationRateForElapsedMonths() {
        String checkingAccount = signupAndFundAccount("fdUser6", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser6", checkingAccount, PRINCIPAL);
        long balanceAfterSubscribe = accountService.getAccount(checkingAccount).getBalance();

        // 6개월 경과 후 중도해지: 이자 = 10,000,000 * 0.5% * 6/12 = 25,000 / 세금 = 3,850
        FixedDepositTerminateResponse response = fixedDepositService.terminate(
                subscribed.getDepositAccountId(), subscribed.getStartDate().plusMonths(6));

        assertEquals("TERMINATED", response.getStatus());
        assertEquals(25_000L, response.getInterestAmount());
        assertEquals(3_850L, response.getTaxAmount());
        assertEquals(PRINCIPAL + 25_000L - 3_850L, response.getPayoutAmount());
        assertEquals(balanceAfterSubscribe + response.getPayoutAmount(),
                accountService.getAccount(checkingAccount).getBalance());
    }

    @Test
    void terminate_onTheSameDayPaysPrincipalOnly() {
        String checkingAccount = signupAndFundAccount("fdUser7", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser7", checkingAccount, PRINCIPAL);

        FixedDepositTerminateResponse response = fixedDepositService.terminate(
                subscribed.getDepositAccountId(), subscribed.getStartDate());

        assertEquals(0L, response.getInterestAmount());
        assertEquals(0L, response.getTaxAmount());
        assertEquals(PRINCIPAL, response.getPayoutAmount());
    }

    @Test
    void terminate_rejectsAccountThatReachedMaturity() {
        String checkingAccount = signupAndFundAccount("fdUser8", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser8", checkingAccount, PRINCIPAL);

        BusinessException e = assertThrows(BusinessException.class, () -> fixedDepositService.terminate(
                subscribed.getDepositAccountId(), subscribed.getMaturityDate()));
        assertEquals(ErrorCode.FIXED_DEPOSIT_MATURITY_REACHED, e.getErrorCode());
    }

    @Test
    void terminate_rejectsAlreadyTerminatedAccount() {
        String checkingAccount = signupAndFundAccount("fdUser9", 20_000_000L);
        FixedDepositSubscribeResponse subscribed = subscribe("fdUser9", checkingAccount, PRINCIPAL);
        LocalDate terminationDate = subscribed.getStartDate().plusMonths(3);
        fixedDepositService.terminate(subscribed.getDepositAccountId(), terminationDate);

        BusinessException e = assertThrows(BusinessException.class,
                () -> fixedDepositService.terminate(subscribed.getDepositAccountId(), terminationDate));
        assertEquals(ErrorCode.FIXED_DEPOSIT_ALREADY_TERMINATED, e.getErrorCode());
    }

    // ---------- 실패 케이스 ----------

    @Test
    void subscribe_rejectsInsufficientBalance() {
        String checkingAccount = signupAndFundAccount("fdUser10", 1_000_000L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscribe("fdUser10", checkingAccount, PRINCIPAL));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, e.getErrorCode());
    }

    @Test
    void subscribe_rejectsAmountBelowProductMinimum() {
        String checkingAccount = signupAndFundAccount("fdUser11", 20_000_000L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscribe("fdUser11", checkingAccount, 500_000L));
        assertEquals(ErrorCode.FIXED_DEPOSIT_AMOUNT_OUT_OF_RANGE, e.getErrorCode());
    }

    @Test
    void subscribe_rejectsTermMismatchWithProduct() {
        String checkingAccount = signupAndFundAccount("fdUser12", 20_000_000L);

        BusinessException e = assertThrows(BusinessException.class, () -> fixedDepositService.subscribe(
                new FixedDepositSubscribeRequest("fdUser12", PRODUCT_ID, checkingAccount, PRINCIPAL, 24)));
        assertEquals(ErrorCode.FIXED_DEPOSIT_INVALID_TERM, e.getErrorCode());
    }

    @Test
    void subscribe_rejectsUnknownProduct() {
        String checkingAccount = signupAndFundAccount("fdUser13", 20_000_000L);

        BusinessException e = assertThrows(BusinessException.class, () -> fixedDepositService.subscribe(
                new FixedDepositSubscribeRequest("fdUser13", "FD_NOT_EXIST", checkingAccount, PRINCIPAL, 12)));
        assertEquals(ErrorCode.FIXED_DEPOSIT_PRODUCT_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void subscribe_rejectsBlankRequiredField() {
        BusinessException e = assertThrows(BusinessException.class, () -> fixedDepositService.subscribe(
                new FixedDepositSubscribeRequest("", PRODUCT_ID, "1001234567", PRINCIPAL, 12)));
        assertEquals(ErrorCode.INVALID_REQUEST, e.getErrorCode());
    }

    @Test
    void getAccount_rejectsUnknownDepositAccount() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> fixedDepositService.getAccount("999999999999"));
        assertEquals(ErrorCode.FIXED_DEPOSIT_ACCOUNT_NOT_FOUND, e.getErrorCode());
    }
}
