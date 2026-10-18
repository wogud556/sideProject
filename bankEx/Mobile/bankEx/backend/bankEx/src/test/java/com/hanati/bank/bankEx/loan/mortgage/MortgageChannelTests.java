package com.hanati.bank.bankEx.loan.mortgage;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.service.accountService;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreClient;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.*;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreException;
import com.hanati.bank.bankEx.loan.mortgage.dto.*;
import com.hanati.bank.bankEx.loan.mortgage.service.mortgageDisbursementService;
import com.hanati.bank.bankEx.loan.mortgage.service.mortgageLoanService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.service.loginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 주담대 고객 채널 ↔ Core 연동. Core는 MortgageCoreClient를 목으로 바꿔 흉내 낸다.
 * 상환 실패 시 출금 롤백을 확인해야 해서 @Transactional을 붙이지 않는다 (테스트마다 새 회원을 만든다).
 */
@SpringBootTest
class MortgageChannelTests {

    @Autowired loginService loginService;
    @Autowired accountService accountService;
    @Autowired transService transService;
    @Autowired mortgageDisbursementService disbursementService;
    @Autowired mortgageLoanService mortgageLoanService;

    @MockitoBean MortgageCoreClient coreClient;

    private String newUser() {
        return "mg" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String signup(String userId) {
        return loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"))
                .getAccountNumber();
    }

    private long balance(String accountNumber) {
        return accountService.getAccount(accountNumber).getBalance();
    }

    // ── 실행금 입금 (Core → bankEx) ──

    @Test
    @DisplayName("실행금 입금: 같은 키로 두 번 와도 한 번만 입금한다")
    void disbursementIsIdempotent() {
        String account = signup(newUser());
        String key = "MGD-MA-TEST-" + UUID.randomUUID();

        MortgageDisbursementResponse first = disbursementService.disburse(
                new MortgageDisbursementRequest(key, account, 300_000_000L, "주택담보대출 실행"));
        MortgageDisbursementResponse second = disbursementService.disburse(
                new MortgageDisbursementRequest(key, account, 300_000_000L, "주택담보대출 실행"));

        assertThat(first.duplicate()).isFalse();
        assertThat(first.transactionNumber()).isEqualTo(key);
        assertThat(second.duplicate()).isTrue();
        assertThat(balance(account)).isEqualTo(300_000_000L);
        assertThat(transService.getTransactions(account)).hasSize(1);
    }

    @Test
    @DisplayName("실행금 입금: 없는 계좌는 거부한다")
    void disbursementToUnknownAccount() {
        assertThatThrownBy(() -> disbursementService.disburse(
                new MortgageDisbursementRequest("MGD-X-" + UUID.randomUUID(), "000-000", 1_000L, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ── 회차 상환 (bankEx 출금 → Core 반영) ──

    private LoanAccount loan(String customerId, long due) {
        return new LoanAccount(
                new Account(7L, "MLN-00000007", customerId, "MG-STD", 300_000_000L, 300_000_000L,
                        new BigDecimal("4.10"), 360, "EQUAL_INSTALLMENT", "2026-10-17", "2056-10-17", "ACTIVE"),
                "정상", "원리금균등",
                new Installment(1, "2026-11-17", 400_000L, due - 400_000L, due, 299_600_000L, "SCHEDULED", null));
    }

    @Test
    @DisplayName("상환: 청구액만큼 출금하고 출금 거래번호를 Core 멱등 키로 넘긴다")
    void repaySuccess() {
        String userId = newUser();
        String account = signup(userId);
        transService.deposit(account, new DepositRequest(2_000_000L, "입금"));
        when(coreClient.getLoanAccount(7L)).thenReturn(loan(userId, 1_450_000L));
        when(coreClient.repay(eq(7L), eq(userId), eq(1_450_000L), eq(account), anyString()))
                .thenReturn(new Repayment(1L, 1, 400_000L, 1_050_000L, 1_450_000L, 299_600_000L, "2026-10-17T10:00:00"));

        MortgageRepayResponse response = mortgageLoanService.repay(7L, new MortgageRepayRequest(userId, account));

        assertThat(response.balanceAfter()).isEqualTo(550_000L);
        assertThat(balance(account)).isEqualTo(550_000L);
        verify(coreClient).repay(eq(7L), eq(userId), eq(1_450_000L), eq(account), eq(response.transactionNumber()));
    }

    @Test
    @DisplayName("상환: Core가 거절하면 출금도 롤백된다")
    void repayRollsBackWhenCoreRejects() {
        String userId = newUser();
        String account = signup(userId);
        transService.deposit(account, new DepositRequest(2_000_000L, "입금"));
        when(coreClient.getLoanAccount(7L)).thenReturn(loan(userId, 1_450_000L));
        when(coreClient.repay(anyLong(), anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new MortgageCoreException("REPAYMENT_AMOUNT_MISMATCH", "상환 금액이 일치하지 않습니다."));

        assertThatThrownBy(() -> mortgageLoanService.repay(7L, new MortgageRepayRequest(userId, account)))
                .isInstanceOf(MortgageCoreException.class);

        assertThat(balance(account)).isEqualTo(2_000_000L);
        assertThat(transService.getTransactions(account)).hasSize(1);
    }

    @Test
    @DisplayName("상환: 잔액 부족이면 Core를 부르지 않는다")
    void repayInsufficientBalance() {
        String userId = newUser();
        String account = signup(userId);
        when(coreClient.getLoanAccount(7L)).thenReturn(loan(userId, 1_450_000L));

        assertThatThrownBy(() -> mortgageLoanService.repay(7L, new MortgageRepayRequest(userId, account)))
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(coreClient, never()).repay(anyLong(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("다른 고객의 대출이나 다른 고객의 출금계좌로는 상환할 수 없다")
    void repayOwnership() {
        String userId = newUser();
        String otherAccount = signup(newUser());
        signup(userId);
        when(coreClient.getLoanAccount(7L)).thenReturn(loan("someone-else", 1_450_000L));
        assertThatThrownBy(() -> mortgageLoanService.repay(7L, new MortgageRepayRequest(userId, otherAccount)))
                .extracting("errorCode").isEqualTo(ErrorCode.MORTGAGE_NOT_OWNED);

        when(coreClient.getLoanAccount(7L)).thenReturn(loan(userId, 1_450_000L));
        assertThatThrownBy(() -> mortgageLoanService.repay(7L, new MortgageRepayRequest(userId, otherAccount)))
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_OWNED);
    }

    // ── 약정 ──

    @Test
    @DisplayName("약정: 실행금 입금 계좌가 본인 계좌가 아니면 Core에 넘기지 않는다")
    void agreementAccountOwnership() {
        String userId = newUser();
        signup(userId);
        String otherAccount = signup(newUser());
        when(coreClient.getApplication(3L)).thenReturn(new ApplicationDetail(
                new ApplicationSummary(3L, "MA-1", userId, "MG-STD", 300_000_000L, "APPROVED", "대출 승인", "2026-10-17"),
                "EQUAL_INSTALLMENT", 360, null, null, null, null));

        assertThatThrownBy(() -> mortgageLoanService.agree(3L, new MortgageAgreementRequest(userId, otherAccount, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_OWNED);
        verify(coreClient, never()).agree(anyLong(), anyString(), anyString(), anyBoolean());
    }
}
