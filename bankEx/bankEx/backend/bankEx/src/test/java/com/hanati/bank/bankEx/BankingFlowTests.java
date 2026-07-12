package com.hanati.bank.bankEx;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.AccountOpenRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.dto.TransactionResponse;
import com.hanati.bank.bankEx.deposit.general.dto.WithdrawRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.loan.general.dto.LoanAccountValidateResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanProductResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentResponse;
import com.hanati.bank.bankEx.loan.general.service.loanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BankingFlowTests {

    @Autowired
    private loginService loginService;
    @Autowired
    private accountService accountService;
    @Autowired
    private transService transService;
    @Autowired
    private loanService loanService;

    private SignupResponse signup(String userId) {
        return loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678"));
    }

    @Test
    void signup_createsActiveAccountWithZeroBalance() {
        SignupResponse response = signup("user1");

        AccountResponse account = accountService.getAccount(response.getAccountNumber());
        assertEquals(0L, account.getBalance());
    }

    @Test
    void deposit_increasesBalanceAndRecordsTransaction() {
        SignupResponse signupResponse = signup("user2");
        String accountNumber = signupResponse.getAccountNumber();

        TransactionResponse response = transService.deposit(accountNumber, new DepositRequest(100_000L, "테스트 입금"));

        assertEquals(100_000L, response.getBalanceAfter());
        assertEquals(100_000L, accountService.getAccount(accountNumber).getBalance());

        List<TransactionResponse> history = transService.getTransactions(accountNumber);
        assertEquals(1, history.size());
        assertEquals("DEPOSIT", history.get(0).getTransactionType());
    }

    @Test
    void deposit_zeroAmount_throwsInvalidAmount() {
        String accountNumber = signup("user3").getAccountNumber();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transService.deposit(accountNumber, new DepositRequest(0L, "무효 입금")));
        assertEquals(ErrorCode.INVALID_AMOUNT, ex.getErrorCode());
    }

    @Test
    void withdraw_insufficientBalance_throws() {
        String accountNumber = signup("user4").getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(10_000L, "입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transService.withdraw(accountNumber, new WithdrawRequest(50_000L, "출금")));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    void withdraw_success_decreasesBalance() {
        String accountNumber = signup("user5").getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(50_000L, "입금"));

        TransactionResponse response = transService.withdraw(accountNumber, new WithdrawRequest(20_000L, "출금"));

        assertEquals(30_000L, response.getBalanceAfter());
    }

    @Test
    void getAccount_notFound_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.getAccount("존재하지않는계좌"));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void openAccount_createsAdditionalAccountForExistingCustomer() {
        String userId = "user6";
        signup(userId);

        AccountResponse account = accountService.openAccount(new AccountOpenRequest(userId, "D002", "저축통장"));

        assertEquals(2, accountService.getAccounts(userId).size());
        assertEquals(0L, account.getBalance());
    }

    @Test
    void loanFlow_validateApproveDisburseRepay() {
        String userId = "user7";
        String accountNumber = signup(userId).getAccountNumber();
        Long productId = loanService.getProducts().get(0).getProductId();

        LoanAccountValidateResponse validation = loanService.validateCustomerAccount(userId, accountNumber);
        assertTrue(validation.isValid());

        LoanApplicationResponse application = loanService.applyLoan(
                new LoanApplicationRequest(userId, accountNumber, productId, 1_000_000L, 12));

        BusinessException notApproved = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(application.getApplicationId())));
        assertEquals(ErrorCode.LOAN_NOT_APPROVED, notApproved.getErrorCode());

        loanService.approveApplication(application.getApplicationId());

        LoanDisbursementResponse disbursement = loanService.disburse(
                new LoanDisbursementRequest(application.getApplicationId()));
        assertEquals(1_000_000L, disbursement.getLoanAmount());
        assertEquals(1_000_000L, accountService.getAccount(accountNumber).getBalance());

        LoanRepaymentResponse repayment = loanService.repay(
                new LoanRepaymentRequest(application.getApplicationId(), 300_000L));
        assertEquals(700_000L, repayment.getRemainingBalance());
        assertEquals(700_000L, accountService.getAccount(accountNumber).getBalance());

        List<LoanProductResponse> products = loanService.getProducts();
        assertTrue(products.size() >= 1);
    }
}
