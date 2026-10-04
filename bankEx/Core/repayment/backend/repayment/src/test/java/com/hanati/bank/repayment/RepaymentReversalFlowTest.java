package com.hanati.bank.repayment;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.dto.*;
import com.hanati.bank.repayment.entity.*;
import com.hanati.bank.repayment.enums.*;
import com.hanati.bank.repayment.repository.*;
import com.hanati.bank.repayment.service.*;
import com.hanati.bank.repayment.support.LoanAccountFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 명세 3.7 / 7.3 상환 취소 + 13.4 역배분 정합성. */
@SpringBootTest
@Transactional
class RepaymentReversalFlowTest {

    @Autowired private LoanAccountFixture fixture;
    @Autowired private RepaymentService repaymentService;
    @Autowired private RepaymentReversalService reversalService;
    @Autowired private OverpaymentService overpaymentService;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private RepaymentScheduleRepository scheduleRepository;
    @Autowired private RepaymentTransactionRepository transactionRepository;
    @Autowired private RepaymentAllocationRepository allocationRepository;
    @Autowired private OverpaymentRepository overpaymentRepository;
    @Autowired private AccountingEventRepository accountingEventRepository;

    private RepaymentRequest req(String type, long amount, String key) {
        return new RepaymentRequest(type, "MANUAL", Money.of(amount), LocalDate.now(), key, "tester");
    }

    private ReversalRequest rev(String key) {
        return new ReversalRequest("고객 이중 납부 확인", key, "tester");
    }

    @Test
    void 취소는_원거래를_삭제하지않고_역거래를_생성한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-1"));

        RepaymentResponse reversed = reversalService.reverse(account.getId(),
                paid.transactionNumber(), rev("rv-r-1"));

        RepaymentTransaction original = transactionRepository
                .findByTransactionNumber(paid.transactionNumber()).orElseThrow();
        assertEquals(RepaymentTransactionStatus.REVERSED, original.getStatus());
        assertEquals(Money.of(878_333L), original.getAllocatedAmount(), "원거래 금액은 수정되지 않는다");

        assertEquals("REPAYMENT_REVERSAL", reversed.transactionType());
        assertEquals(Money.of(-878_333L), reversed.allocatedAmount());
        assertEquals(2, transactionRepository.findByLoanAccountIdOrderByIdDesc(account.getId()).size());
    }

    @Test
    void 취소후_원금과_이자_잔액이_원상복구된다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        RepaymentSchedule schedule = fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-2"));
        assertEquals(Money.of(9_166_667L), paid.remainingPrincipal());

        reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-2"));

        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(Money.of(10_000_000L), after.getPrincipalBalance());
        assertEquals(Money.of(45_000L), after.getAccruedInterest());

        RepaymentSchedule s = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals(Money.ZERO, s.getPaidPrincipal());
        assertEquals(Money.ZERO, s.getPaidInterest());
        assertEquals(ScheduleStatus.SCHEDULED, s.getStatus());
        assertNull(s.getPaidAt());
    }

    @Test
    void 연체계좌_취소는_연체버킷과_비용잔액까지_복구한다() {
        LoanAccount account = fixture.account(10_000_000L, 10_000L, BigDecimal.ZERO,
                "110-000-111111", LocalDate.now().minusMonths(3));
        RepaymentSchedule overdue = fixture.overdueSchedule(account, 1,
                LocalDate.now().minusMonths(2), 833_333L, 20_000L);

        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("MANUAL_REPAYMENT", 863_333L, "rv-k-3"));
        LoanAccount mid = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(Money.ZERO, mid.getFeeBalance());
        assertEquals(Money.ZERO, mid.getOverduePrincipal());

        reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-3"));

        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(Money.of(10_000L), after.getFeeBalance());
        assertEquals(Money.of(833_333L), after.getOverduePrincipal());
        assertEquals(Money.of(20_000L), after.getOverdueInterest());
        assertEquals(LoanAccountStatus.DELINQUENT, after.getStatus(), "거래 직전 상태로 복원된다");

        RepaymentSchedule s = scheduleRepository.findById(overdue.getId()).orElseThrow();
        assertEquals(ScheduleStatus.OVERDUE, s.getStatus());
    }

    @Test
    void 완제취소는_계좌상태를_거래직전값으로_되돌린다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse paid = repaymentService.repayInFull(account.getId(),
                new FullRepaymentRequest(Money.of(1_000_000L), "MANUAL", LocalDate.now(), "rv-k-4", "t"));
        assertEquals("PAID_OFF", paid.loanAccountStatus());

        RepaymentResponse reversed = reversalService.reverse(account.getId(),
                paid.transactionNumber(), rev("rv-r-4"));

        assertEquals("ACTIVE", reversed.loanAccountStatus());
        assertEquals(Money.of(1_000_000L), reversed.remainingPrincipal());
    }

    @Test
    void 이미_취소된거래는_다시_취소할수없다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-5"));
        reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-5"));

        BusinessException e = assertThrows(BusinessException.class, () -> reversalService.reverse(
                account.getId(), paid.transactionNumber(), rev("rv-r-5b")));
        assertEquals(ErrorCode.REPAYMENT_ALREADY_REVERSED, e.getErrorCode());
    }

    @Test
    void 후속거래가_있는_원거래는_취소할수없다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.equalPrincipalSchedules(account, 12, 10_000_000L, 45_000L, LocalDate.now().minusMonths(2));
        RepaymentResponse first = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 100_000L, "rv-k-6a"));
        repaymentService.repay(account.getId(), req("REGULAR_REPAYMENT", 100_000L, "rv-k-6b"));

        BusinessException e = assertThrows(BusinessException.class, () -> reversalService.reverse(
                account.getId(), first.transactionNumber(), rev("rv-r-6")));
        assertEquals(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED, e.getErrorCode());
    }

    @Test
    void 다른계좌의_거래는_취소할수없다() {
        LoanAccount a = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(a, 833_333L, 45_000L);
        LoanAccount b = fixture.simpleAccount(10_000_000L);
        RepaymentResponse paid = repaymentService.repay(a.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-7"));

        BusinessException e = assertThrows(BusinessException.class, () -> reversalService.reverse(
                b.getId(), paid.transactionNumber(), rev("rv-r-7")));
        assertEquals(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED, e.getErrorCode());
    }

    @Test
    void 없는_거래번호는_취소할수없다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        BusinessException e = assertThrows(BusinessException.class, () -> reversalService.reverse(
                account.getId(), "RP00000000000000", rev("rv-r-8")));
        assertEquals(ErrorCode.REPAYMENT_TRANSACTION_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void 환급완료된_과오납이_있으면_취소할수없다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_500_000L, "rv-k-9"));
        overpaymentService.refund(account.getId(), paid.overpaymentId());

        BusinessException e = assertThrows(BusinessException.class, () -> reversalService.reverse(
                account.getId(), paid.transactionNumber(), rev("rv-r-9")));
        assertEquals(ErrorCode.REPAYMENT_REVERSAL_NOT_ALLOWED, e.getErrorCode());
    }

    @Test
    void 환급대기_과오납은_취소와_함께_REFUND_CANCELLED가_된다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_500_000L, "rv-k-10"));

        reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-10"));

        Overpayment o = overpaymentRepository.findById(paid.overpaymentId()).orElseThrow();
        assertEquals(OverpaymentStatus.REFUND_CANCELLED, o.getStatus());
    }

    @Test
    void 원거래_배분합계와_취소거래_역배분합계가_일치한다() {
        LoanAccount account = fixture.account(10_000_000L, 10_000L, BigDecimal.ZERO,
                "110-000-111111", LocalDate.now().minusMonths(3));
        fixture.overdueSchedule(account, 1, LocalDate.now().minusMonths(2), 833_333L, 20_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("MANUAL_REPAYMENT", 500_000L, "rv-k-11"));
        RepaymentResponse reversed = reversalService.reverse(account.getId(),
                paid.transactionNumber(), rev("rv-r-11"));

        Long originalId = transactionRepository
                .findByTransactionNumber(paid.transactionNumber()).orElseThrow().getId();
        Long reversalId = transactionRepository
                .findByTransactionNumber(reversed.transactionNumber()).orElseThrow().getId();

        BigDecimal originalSum = sum(originalId);
        BigDecimal reversalSum = sum(reversalId);
        assertEquals(originalSum, reversalSum.negate());
        assertEquals(Money.of(500_000L), originalSum);
    }

    @Test
    void 취소는_반대_회계이벤트를_적재한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-12"));
        RepaymentResponse reversed = reversalService.reverse(account.getId(),
                paid.transactionNumber(), rev("rv-r-12"));

        Long reversalId = transactionRepository
                .findByTransactionNumber(reversed.transactionNumber()).orElseThrow().getId();
        List<AccountingEvent> events =
                accountingEventRepository.findByRepaymentTransactionIdOrderByIdAsc(reversalId);

        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AccountingEventType.REPAYMENT_REVERSED));
        assertTrue(events.stream()
                .filter(e -> e.getEventType() == AccountingEventType.LOAN_PRINCIPAL_REPAID)
                .allMatch(e -> e.getAmount().signum() < 0), "원금 상환 이벤트는 음수로 적재된다");
    }

    @Test
    void 동일_멱등성키로_취소를_재요청하면_같은_취소거래를_돌려준다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse paid = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "rv-k-13"));

        RepaymentResponse r1 = reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-13"));
        RepaymentResponse r2 = reversalService.reverse(account.getId(), paid.transactionNumber(), rev("rv-r-13"));

        assertEquals(r1.transactionNumber(), r2.transactionNumber());
        assertEquals(Money.of(10_000_000L),
                loanAccountRepository.findById(account.getId()).orElseThrow().getPrincipalBalance());
    }

    private BigDecimal sum(Long transactionId) {
        return allocationRepository.findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(transactionId)
                .stream()
                .map(RepaymentAllocation::getAllocatedAmount)
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
