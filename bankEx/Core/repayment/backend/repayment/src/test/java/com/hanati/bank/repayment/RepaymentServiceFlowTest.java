package com.hanati.bank.repayment;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.dto.*;
import com.hanati.bank.repayment.entity.*;
import com.hanati.bank.repayment.enums.*;
import com.hanati.bank.repayment.gateway.DemoScenarioAccounts;
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

/** 명세 13.2 상환 서비스 테스트 + 13.4 정합성 검증. */
@SpringBootTest
@Transactional
class RepaymentServiceFlowTest {

    @Autowired private LoanAccountFixture fixture;
    @Autowired private RepaymentService repaymentService;
    @Autowired private RepaymentReversalService reversalService;
    @Autowired private RepaymentQuoteService quoteService;
    @Autowired private OverpaymentService overpaymentService;
    @Autowired private RepaymentHistoryService historyService;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private RepaymentScheduleRepository scheduleRepository;
    @Autowired private RepaymentAllocationRepository allocationRepository;
    @Autowired private RepaymentTransactionRepository transactionRepository;
    @Autowired private OverpaymentRepository overpaymentRepository;
    @Autowired private AccountingEventRepository accountingEventRepository;

    private RepaymentRequest req(String type, long amount, String key) {
        return new RepaymentRequest(type, "MANUAL", Money.of(amount), LocalDate.now(), key, "tester");
    }

    // ---------- 정기 상환 ----------

    @Test
    void 정기상환은_이자먼저_원금나중에_배분하고_원금잔액을_줄인다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "k-regular-1"));

        assertEquals("COMPLETED", r.status());
        assertEquals(Money.of(878_333L), r.allocatedAmount());
        assertEquals(Money.ZERO, r.overpaymentAmount());
        assertEquals(Money.of(10_000_000L - 833_333L), r.remainingPrincipal());

        List<AllocationResponse> a = r.allocations();
        assertEquals("INTEREST", a.get(0).type());
        assertEquals(Money.of(45_000L), a.get(0).amount());
        assertEquals("PRINCIPAL", a.get(1).type());
        assertEquals(Money.of(833_333L), a.get(1).amount());
    }

    @Test
    void 부분상환은_가능한항목까지만_배분하고_미납분은_잔존채무로_남는다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        RepaymentSchedule schedule = fixture.dueSchedule(account, 833_333L, 45_000L);

        // 이자 45,000 + 원금 55,000 만 납부
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PARTIAL_REPAYMENT", 100_000L, "k-partial-1"));

        assertEquals(Money.of(100_000L), r.allocatedAmount());
        RepaymentSchedule after = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals(ScheduleStatus.PARTIALLY_PAID, after.getStatus());
        assertEquals(Money.of(45_000L), after.getPaidInterest());
        assertEquals(Money.of(55_000L), after.getPaidPrincipal());
        assertEquals(Money.of(833_333L - 55_000L), after.outstandingPrincipal());
    }

    @Test
    void 연체계좌는_비용_연체이자_연체원금_순서로_배분한다() {
        LoanAccount account = fixture.account(10_000_000L, 10_000L, BigDecimal.ZERO,
                "110-000-111111", LocalDate.now().minusMonths(3));
        fixture.overdueSchedule(account, 1, LocalDate.now().minusMonths(2), 833_333L, 20_000L);
        fixture.overdueSchedule(account, 2, LocalDate.now().minusMonths(1), 833_333L, 15_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("MANUAL_REPAYMENT", 1_000_000L, "k-delinquent-1"));

        List<AllocationResponse> a = r.allocations();
        assertEquals("FEE", a.get(0).type());
        assertEquals(Money.of(10_000L), a.get(0).amount());
        assertEquals("OVERDUE_INTEREST", a.get(1).type());
        assertEquals("OVERDUE_PRINCIPAL", a.get(2).type());
        assertEquals(Money.of(833_333L), a.get(2).amount());
        // 2회차로 넘어가 연체이자 → 연체원금
        assertEquals("OVERDUE_INTEREST", a.get(3).type());
        assertEquals("OVERDUE_PRINCIPAL", a.get(4).type());
        assertEquals(Money.of(1_000_000L), r.allocatedAmount());
    }

    // ---------- 중도상환 ----------

    @Test
    void 중도상환은_원금만_감소시키고_스케줄_예정금액은_바뀌지_않는다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        List<RepaymentSchedule> schedules =
                fixture.equalPrincipalSchedules(account, 12, 10_000_000L, 45_000L, LocalDate.now());

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 2_045_000L, "k-prepay-1"));

        // 도래한 1회차 이자 45,000 수취 후 원금 200만 감소
        assertEquals(Money.of(45_000L), sumOf(r, "INTEREST"));
        assertEquals(Money.of(2_000_000L), sumOf(r, "PRINCIPAL"));
        assertEquals(Money.of(8_000_000L), r.remainingPrincipal());

        // 2회차 이후 예정금액은 재산정되지 않는다 (정책 ③)
        RepaymentSchedule second = scheduleRepository.findById(schedules.get(1).getId()).orElseThrow();
        assertEquals(Money.of(833_333L), second.getScheduledPrincipal());
        assertEquals(Money.of(45_000L), second.getScheduledInterest());
        assertEquals(ScheduleStatus.SCHEDULED, second.getStatus());
    }

    @Test
    void 중도상환수수료는_비용으로_배분된다() {
        LoanAccount account = fixture.account(10_000_000L, 0L, new BigDecimal("1.20"),
                "110-000-333333", LocalDate.now().minusMonths(1));
        fixture.dueSchedule(account, 833_333L, 45_000L);

        // 원금 100만 중도상환 → 수수료 1.2% = 12,000
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_000_000L, "k-prepay-fee-1"));

        assertEquals(Money.of(12_000L), sumOf(r, "FEE"));
        assertEquals(Money.of(45_000L), sumOf(r, "INTEREST"));
        assertEquals(Money.of(943_000L), sumOf(r, "PRINCIPAL"));
        assertEquals(Money.of(1_000_000L), r.allocatedAmount());
    }

    // ---------- 전액 상환 ----------

    @Test
    void 전액상환은_잔존채무를_0으로만들고_완제처리한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        BigDecimal total = quoteService.quote(account.getId(),
                RepaymentTransactionType.FULL_REPAYMENT, LocalDate.now(), null).totalAmount();
        assertEquals(Money.of(10_045_000L), total);

        RepaymentResponse r = repaymentService.repayInFull(account.getId(),
                new FullRepaymentRequest(total, "MANUAL", LocalDate.now(), "k-full-1", "tester"));

        assertEquals("PAID_OFF", r.loanAccountStatus());
        assertEquals(Money.ZERO, r.remainingPrincipal());
        assertEquals(Money.ZERO, r.totalOutstanding());
        assertTrue(accountingEventRepository.findByRepaymentTransactionIdOrderByIdAsc(
                        transactionRepository.findByTransactionNumber(r.transactionNumber()).orElseThrow().getId())
                .stream().anyMatch(e -> e.getEventType() == AccountingEventType.LOAN_PAID_OFF));
    }

    @Test
    void 전액상환_예정금액이_달라지면_거절한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> repaymentService.repayInFull(account.getId(),
                        new FullRepaymentRequest(Money.of(9_000_000L), "MANUAL", LocalDate.now(),
                                "k-full-stale", "tester")));
        assertEquals(ErrorCode.FULL_REPAYMENT_AMOUNT_CHANGED, e.getErrorCode());
    }

    @Test
    void 완제된계좌는_추가상환할수없다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        repaymentService.repayInFull(account.getId(),
                new FullRepaymentRequest(Money.of(1_000_000L), "MANUAL", LocalDate.now(), "k-full-2", "t"));

        BusinessException e = assertThrows(BusinessException.class, () -> repaymentService.repay(
                account.getId(), req("MANUAL_REPAYMENT", 10_000L, "k-after-paidoff")));
        assertEquals(ErrorCode.INVALID_LOAN_STATUS, e.getErrorCode());
    }

    // ---------- 과오납 ----------

    @Test
    void 채무보다_큰금액은_과오납으로_분리되고_원금은_음수가되지않는다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 50_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 2_000_000L, "k-over-1"));

        assertEquals(Money.of(1_050_000L), r.allocatedAmount());
        assertEquals(Money.of(950_000L), r.overpaymentAmount());
        assertEquals(Money.ZERO, r.remainingPrincipal());
        assertNotNull(r.overpaymentId());

        Overpayment o = overpaymentRepository.findById(r.overpaymentId()).orElseThrow();
        assertEquals(OverpaymentStatus.PENDING_REFUND, o.getStatus());
        assertEquals(Money.of(950_000L), o.getAmount());
    }

    @Test
    void 과오납_환급은_상태를_REFUNDED로_바꾼다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_500_000L, "k-over-2"));

        OverpaymentRefundResponse refund = overpaymentService.refund(account.getId(), r.overpaymentId());

        assertEquals("REFUNDED", refund.status());
        assertNotNull(refund.refundTransactionNumber());
        assertEquals(Money.of(500_000L), refund.amount());
    }

    @Test
    void 이미_환급된_과오납은_다시_환급할수없다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_500_000L, "k-over-3"));
        overpaymentService.refund(account.getId(), r.overpaymentId());

        BusinessException e = assertThrows(BusinessException.class,
                () -> overpaymentService.refund(account.getId(), r.overpaymentId()));
        assertEquals(ErrorCode.OVERPAYMENT_ALREADY_REFUNDED, e.getErrorCode());
    }

    @Test
    void 환급이_실패하면_REFUND_FAILED로_남는다() {
        LoanAccount account = fixture.account(1_000_000L, 0L, BigDecimal.ZERO,
                DemoScenarioAccounts.REFUND_FAILURE_ACCOUNT, LocalDate.now().minusMonths(1));
        fixture.dueSchedule(account, 1_000_000L, 0L);
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 1_500_000L, "k-over-4"));

        OverpaymentRefundResponse refund = overpaymentService.refund(account.getId(), r.overpaymentId());
        assertEquals("REFUND_FAILED", refund.status());
        assertNull(refund.refundTransactionNumber());
    }

    // ---------- 멱등성 ----------

    @Test
    void 동일_멱등성키_재요청은_같은거래를_돌려주고_두번처리하지않는다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        RepaymentResponse first = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "k-idem-1"));
        RepaymentResponse second = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "k-idem-1"));

        assertEquals(first.transactionNumber(), second.transactionNumber());
        assertEquals(1, transactionRepository.findByLoanAccountIdOrderByIdDesc(account.getId()).size());
        assertEquals(Money.of(10_000_000L - 833_333L),
                loanAccountRepository.findById(account.getId()).orElseThrow().getPrincipalBalance());
    }

    @Test
    void 같은키로_다른금액이_오면_거절한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        repaymentService.repay(account.getId(), req("REGULAR_REPAYMENT", 100_000L, "k-idem-2"));

        BusinessException e = assertThrows(BusinessException.class, () -> repaymentService.repay(
                account.getId(), req("REGULAR_REPAYMENT", 200_000L, "k-idem-2")));
        assertEquals(ErrorCode.DUPLICATE_REPAYMENT_REQUEST, e.getErrorCode());
    }

    // ---------- 자동이체 ----------

    @Test
    void 자동이체는_출금게이트웨이를_호출하고_금액생략시_도래분전액을_출금한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(), new RepaymentRequest(
                "REGULAR_REPAYMENT", "AUTO_DEBIT", null, LocalDate.now(), "k-auto-1", "batch"));

        assertEquals(Money.of(878_333L), r.requestedAmount());
        assertEquals(Money.ZERO, r.overpaymentAmount());
    }

    @Test
    void 자동이체_출금실패시_상환이_기록되지않는다() {
        LoanAccount account = fixture.account(10_000_000L, 0L, BigDecimal.ZERO,
                DemoScenarioAccounts.WITHDRAWAL_FAILURE_ACCOUNT, LocalDate.now().minusMonths(1));
        fixture.dueSchedule(account, 833_333L, 45_000L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> repaymentService.repay(account.getId(), new RepaymentRequest(
                        "REGULAR_REPAYMENT", "AUTO_DEBIT", null, LocalDate.now(), "k-auto-fail", "batch")));
        assertEquals(ErrorCode.INSUFFICIENT_WITHDRAWAL_BALANCE, e.getErrorCode());
        assertTrue(transactionRepository.findByLoanAccountIdOrderByIdDesc(account.getId()).isEmpty());
    }

    // ---------- 검증 규칙 ----------

    @Test
    void 금액이_0이하면_거절한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        BusinessException e = assertThrows(BusinessException.class, () -> repaymentService.repay(
                account.getId(), req("REGULAR_REPAYMENT", 0L, "k-zero")));
        assertEquals(ErrorCode.INVALID_REPAYMENT_AMOUNT, e.getErrorCode());
    }

    @Test
    void 없는_대출계좌는_상환할수없다() {
        BusinessException e = assertThrows(BusinessException.class, () -> repaymentService.repay(
                999_999L, req("REGULAR_REPAYMENT", 100_000L, "k-nf")));
        assertEquals(ErrorCode.LOAN_ACCOUNT_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void 상환일이_대출실행일보다_빠르면_거절한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> repaymentService.repay(account.getId(), new RepaymentRequest(
                        "REGULAR_REPAYMENT", "MANUAL", Money.of(100_000L),
                        account.getDisbursementDate().minusDays(1), "k-early", "t")));
        assertEquals(ErrorCode.INVALID_PAYMENT_DATE, e.getErrorCode());
    }

    @Test
    void 전액상환은_전용API를_써야한다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        BusinessException e = assertThrows(BusinessException.class, () -> repaymentService.repay(
                account.getId(), req("FULL_REPAYMENT", 100_000L, "k-wrong-api")));
        assertEquals(ErrorCode.INVALID_REQUEST, e.getErrorCode());
    }

    // ---------- 정합성 (명세 13.4) ----------

    @Test
    void 요청금액은_배분합계와_과오납합계의_합과_같다() {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.dueSchedule(account, 1_000_000L, 50_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("PREPAYMENT", 2_000_000L, "k-inv-1"));

        assertEquals(r.requestedAmount(), r.allocatedAmount().add(r.overpaymentAmount()));
    }

    @Test
    void 스케줄_납부금액은_해당스케줄_배분내역합계와_같다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        RepaymentSchedule schedule = fixture.dueSchedule(account, 833_333L, 45_000L);

        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 500_000L, "k-inv-2"));

        RepaymentTransaction txn = transactionRepository
                .findByTransactionNumber(r.transactionNumber()).orElseThrow();
        List<RepaymentAllocation> allocations = allocationRepository
                .findByRepaymentTransactionIdOrderByAllocationOrderAscIdAsc(txn.getId());

        BigDecimal allocatedToSchedule = allocations.stream()
                .filter(a -> schedule.getId().equals(a.getRepaymentScheduleId()))
                .map(RepaymentAllocation::getAllocatedAmount)
                .reduce(Money.ZERO, BigDecimal::add);

        RepaymentSchedule after = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals(after.getPaidPrincipal().add(after.getPaidInterest()), allocatedToSchedule);
    }

    @Test
    void 거래내역_조회는_배분내역까지_돌려준다() {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.dueSchedule(account, 833_333L, 45_000L);
        RepaymentResponse r = repaymentService.repay(account.getId(),
                req("REGULAR_REPAYMENT", 878_333L, "k-hist-1"));

        RepaymentTransactionResponse one = historyService.getTransaction(account.getId(), r.transactionNumber());
        assertEquals(2, one.allocations().size());
        assertEquals(1, historyService.getTransactions(account.getId()).size());
    }

    private BigDecimal sumOf(RepaymentResponse r, String type) {
        return r.allocations().stream()
                .filter(a -> a.type().equals(type))
                .map(AllocationResponse::amount)
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
