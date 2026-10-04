package com.hanati.bank.bankEx;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseRequest;
import com.hanati.bank.bankEx.deposit.general.dto.AccountCloseResponse;
import com.hanati.bank.bankEx.deposit.general.dto.AccountResponse;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.dto.WithdrawRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentRequest;
import com.hanati.bank.bankEx.loan.general.service.loanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AccountCloseFlowTests {

    @Autowired
    private loginService loginService;
    @Autowired
    private accountService accountService;
    @Autowired
    private transService transService;
    @Autowired
    private loanService loanService;

    private SignupResponse signup(String userId) {
        return loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"));
    }

    @Test
    void closeAccount_zeroBalance_success() {
        String userId = "close1";
        String accountNumber = signup(userId).getAccountNumber();

        AccountCloseResponse response = accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));

        assertEquals("CLOSED", response.getAccountStatus());
        assertNotNull(response.getClosedAt());
    }

    @Test
    void closeAccount_nonZeroBalance_throwsBalanceNotZero() {
        String userId = "close2";
        String accountNumber = signup(userId).getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(10_000L, "입금"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.closeAccount(accountNumber, new AccountCloseRequest(userId)));
        assertEquals(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO, ex.getErrorCode());
    }

    @Test
    void closeAccount_afterDepositAndWithdrawBackToZero_success() {
        String userId = "close3";
        String accountNumber = signup(userId).getAccountNumber();
        transService.deposit(accountNumber, new DepositRequest(20_000L, "입금"));
        transService.withdraw(accountNumber, new WithdrawRequest(20_000L, "출금"));

        AccountCloseResponse response = accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));

        assertEquals("CLOSED", response.getAccountStatus());
    }

    @Test
    void closeAccount_alreadyClosed_throwsAlreadyClosed() {
        String userId = "close4";
        String accountNumber = signup(userId).getAccountNumber();
        accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.closeAccount(accountNumber, new AccountCloseRequest(userId)));
        assertEquals(ErrorCode.ACCOUNT_ALREADY_CLOSED, ex.getErrorCode());
    }

    @Test
    void closeAccount_accountNotFound_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.closeAccount("존재하지않는계좌", new AccountCloseRequest("close5")));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void closeAccount_ownerMismatch_throwsAccountNotFound() {
        String userId = "close6";
        String otherUserId = "close6b";
        String accountNumber = signup(userId).getAccountNumber();
        signup(otherUserId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.closeAccount(accountNumber, new AccountCloseRequest(otherUserId)));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void closedAccount_depositAndWithdraw_stillBlockedByAccountNotActive() {
        String userId = "close7";
        String accountNumber = signup(userId).getAccountNumber();
        accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));

        BusinessException depositEx = assertThrows(BusinessException.class,
                () -> transService.deposit(accountNumber, new DepositRequest(1_000L, "입금")));
        assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, depositEx.getErrorCode());

        BusinessException withdrawEx = assertThrows(BusinessException.class,
                () -> transService.withdraw(accountNumber, new WithdrawRequest(1_000L, "출금")));
        assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, withdrawEx.getErrorCode());
    }

    @Test
    void closeAccount_thenGetAccounts_showsClosedStatus() {
        String userId = "close8";
        String accountNumber = signup(userId).getAccountNumber();
        accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));

        List<AccountResponse> accounts = accountService.getAccounts(userId);
        assertEquals(1, accounts.size());
        assertEquals("CLOSED", accounts.get(0).getAccountStatus());
    }

    @Test
    void closeAccount_withActiveLoanApplication_throwsHasActiveLoan() {
        String userId = "close9";
        String accountNumber = signup(userId).getAccountNumber();
        Long productId = loanService.getProducts().get(0).getProductId();

        loanService.applyLoan(new LoanApplicationRequest(userId, accountNumber, productId, 1_000_000L, 12, "EQUAL_PRINCIPAL"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.closeAccount(accountNumber, new AccountCloseRequest(userId)));
        assertEquals(ErrorCode.ACCOUNT_HAS_ACTIVE_LOAN, ex.getErrorCode());
    }

    @Test
    void closeAccount_afterLoanFullyRepaid_success() {
        String userId = "close10";
        String accountNumber = signup(userId).getAccountNumber();
        Long productId = loanService.getProducts().get(0).getProductId();

        LoanApplicationResponse application = loanService.applyLoan(
                new LoanApplicationRequest(userId, accountNumber, productId, 1_000_000L, 12, "EQUAL_PRINCIPAL"));
        loanService.approveApplication(application.getApplicationId());
        loanService.disburse(new LoanDisbursementRequest(application.getApplicationId()));
        loanService.repay(new LoanRepaymentRequest(application.getApplicationId(), 1_000_000L));

        assertEquals(0L, accountService.getAccount(accountNumber).getBalance());

        AccountCloseResponse response = accountService.closeAccount(accountNumber, new AccountCloseRequest(userId));
        assertEquals("CLOSED", response.getAccountStatus());
    }
}
