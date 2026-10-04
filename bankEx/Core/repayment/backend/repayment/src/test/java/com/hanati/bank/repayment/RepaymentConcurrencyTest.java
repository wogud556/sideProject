package com.hanati.bank.repayment;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.dto.FullRepaymentRequest;
import com.hanati.bank.repayment.dto.RepaymentRequest;
import com.hanati.bank.repayment.dto.RepaymentResponse;
import com.hanati.bank.repayment.dto.ReversalRequest;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.enums.LoanAccountStatus;
import com.hanati.bank.repayment.enums.RepaymentTransactionStatus;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import com.hanati.bank.repayment.repository.*;
import com.hanati.bank.repayment.service.RepaymentReversalService;
import com.hanati.bank.repayment.service.RepaymentService;
import com.hanati.bank.repayment.support.LoanAccountFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 명세 13.3 동시성 테스트.
 *
 * <p>클래스 레벨 @Transactional을 쓰지 않는다. 테스트 트랜잭션으로 감싸면 모든 호출이 같은
 * 트랜잭션에 참여해 비관적 잠금 경합이 재현되지 않는다.
 */
@SpringBootTest
class RepaymentConcurrencyTest {

    @Autowired private LoanAccountFixture fixture;
    @Autowired private RepaymentService repaymentService;
    @Autowired private RepaymentReversalService reversalService;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private RepaymentTransactionRepository transactionRepository;

    private record Outcome(boolean success, String detail) {
    }

    private List<Outcome> runConcurrently(int threads, IntFunction<String> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int index = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return new Outcome(true, task.apply(index));
                } catch (Exception e) {
                    Throwable root = e;
                    while (root.getCause() != null && !(root instanceof BusinessException)) {
                        root = root.getCause();
                    }
                    return new Outcome(false, root.getClass().getSimpleName() + ": " + root.getMessage());
                }
            }));
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        List<Outcome> results = new ArrayList<>();
        for (Future<Outcome> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    private RepaymentRequest req(String type, long amount, String key) {
        return new RepaymentRequest(type, "MANUAL", Money.of(amount), LocalDate.now(), key, "tester");
    }

    @Test
    void 동일계좌에_두상환이_동시에들어와도_원금이_정확히_차감된다() throws Exception {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.equalPrincipalSchedules(account, 12, 10_000_000L, 0L, LocalDate.now().minusMonths(11));

        List<Outcome> results = runConcurrently(2, i ->
                repaymentService.repay(account.getId(),
                        req("MANUAL_REPAYMENT", 1_000_000L, "conc-a-" + i)).status());

        assertEquals(2, results.stream().filter(Outcome::success).count(),
                () -> "실패 내역: " + results);
        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(Money.of(8_000_000L), after.getPrincipalBalance());
    }

    @Test
    void 남은원금보다_큰_두요청이_동시에들어와도_원금은_음수가되지않는다() throws Exception {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.equalPrincipalSchedules(account, 1, 1_000_000L, 0L, LocalDate.now());

        // 각각 80만원 → 합 160만이 원금 100만을 초과한다
        List<Outcome> results = runConcurrently(2, i ->
                repaymentService.repay(account.getId(),
                        req("MANUAL_REPAYMENT", 800_000L, "conc-b-" + i)).status());

        assertEquals(2, results.stream().filter(Outcome::success).count(), () -> "실패 내역: " + results);
        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, after.getPrincipalBalance().signum(), "원금잔액이 음수가 되지 않는다");
        assertTrue(after.totalOutstanding().signum() >= 0);

        // 초과분은 과오납으로 분리되어 요청 총액이 보존된다
        BigDecimal allocated = transactionRepository.findByLoanAccountIdOrderByIdDesc(account.getId())
                .stream().map(t -> t.getAllocatedAmount().add(t.getOverpaymentAmount()))
                .reduce(Money.ZERO, BigDecimal::add);
        assertEquals(Money.of(1_600_000L), allocated);
    }

    @Test
    void 동일_멱등성키_동시요청은_한번만_처리된다() throws Exception {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.equalPrincipalSchedules(account, 12, 10_000_000L, 0L, LocalDate.now().minusMonths(11));

        List<Outcome> results = runConcurrently(4, i ->
                repaymentService.repay(account.getId(),
                        req("MANUAL_REPAYMENT", 500_000L, "conc-same-key")).status());

        long completed = transactionRepository.findByLoanAccountIdOrderByIdDesc(account.getId()).stream()
                .filter(t -> t.getStatus() == RepaymentTransactionStatus.COMPLETED)
                .count();
        assertEquals(1, completed, () -> "거래가 1건만 남아야 한다. 결과: " + results);

        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(Money.of(9_500_000L), after.getPrincipalBalance(), "한 번만 차감된다");
    }

    @Test
    void 전액상환과_일반상환이_동시에들어오면_완제후_추가차감이_없다() throws Exception {
        LoanAccount account = fixture.simpleAccount(1_000_000L);
        fixture.equalPrincipalSchedules(account, 1, 1_000_000L, 0L, LocalDate.now());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Outcome> full = pool.submit(() -> {
            start.await();
            try {
                return new Outcome(true, repaymentService.repayInFull(account.getId(),
                        new FullRepaymentRequest(Money.of(1_000_000L), "MANUAL", LocalDate.now(),
                                "conc-full", "t")).loanAccountStatus());
            } catch (Exception e) {
                return new Outcome(false, e.getClass().getSimpleName());
            }
        });
        Future<Outcome> regular = pool.submit(() -> {
            start.await();
            try {
                return new Outcome(true, repaymentService.repay(account.getId(),
                        req("MANUAL_REPAYMENT", 300_000L, "conc-regular")).status());
            } catch (Exception e) {
                return new Outcome(false, e.getClass().getSimpleName());
            }
        });
        start.countDown();
        Outcome fullResult = full.get(30, TimeUnit.SECONDS);
        Outcome regularResult = regular.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertTrue(after.getPrincipalBalance().signum() >= 0, "원금잔액이 음수가 되지 않는다");

        // 둘 중 하나만 반영되어야 한다. 완제가 먼저 끝나면 일반 상환은 INVALID_LOAN_STATUS로 막히고,
        // 일반 상환이 먼저 끝나면 전액상환 금액이 바뀌므로 FULL_REPAYMENT_AMOUNT_CHANGED로 막힌다.
        assertFalse(fullResult.success() && regularResult.success(),
                "둘 다 성공하면 완제 계좌에 추가 차감이 일어난 것이다: " + fullResult + " / " + regularResult);
        assertTrue(fullResult.success() || regularResult.success(),
                "적어도 하나는 처리되어야 한다: " + fullResult + " / " + regularResult);

        if (fullResult.success()) {
            assertEquals(Money.of(0L), after.getPrincipalBalance(), "전액상환이 반영되면 완제된다");
            assertEquals(LoanAccountStatus.PAID_OFF, after.getStatus());
        } else {
            assertEquals(Money.of(700_000L), after.getPrincipalBalance(),
                    "일반상환 30만만 반영되고 전액상환은 금액 변경으로 거절된다");
            assertEquals(LoanAccountStatus.ACTIVE, after.getStatus());
        }
    }

    @Test
    void 상환과_상환취소가_동시에들어와도_잔액정합성이_유지된다() throws Exception {
        LoanAccount account = fixture.simpleAccount(10_000_000L);
        fixture.equalPrincipalSchedules(account, 12, 10_000_000L, 0L, LocalDate.now().minusMonths(11));
        RepaymentResponse first = repaymentService.repay(account.getId(),
                req("MANUAL_REPAYMENT", 1_000_000L, "conc-rv-base"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Outcome> reverse = pool.submit(() -> {
            start.await();
            try {
                return new Outcome(true, reversalService.reverse(account.getId(), first.transactionNumber(),
                        new ReversalRequest("동시 취소", "conc-rv-r", "t")).status());
            } catch (Exception e) {
                return new Outcome(false, e.getClass().getSimpleName());
            }
        });
        Future<Outcome> repay = pool.submit(() -> {
            start.await();
            try {
                return new Outcome(true, repaymentService.repay(account.getId(),
                        req("MANUAL_REPAYMENT", 500_000L, "conc-rv-p")).status());
            } catch (Exception e) {
                return new Outcome(false, e.getClass().getSimpleName());
            }
        });
        start.countDown();
        Outcome reverseResult = reverse.get(30, TimeUnit.SECONDS);
        Outcome repayResult = repay.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        LoanAccount after = loanAccountRepository.findById(account.getId()).orElseThrow();
        assertTrue(after.getPrincipalBalance().signum() >= 0);

        // 완료된 거래들의 원금 배분 합계가 실제 원금 차감액과 일치해야 한다
        BigDecimal expectedBalance = Money.of(10_000_000L);
        if (repayResult.success()) {
            expectedBalance = expectedBalance.subtract(Money.of(500_000L));
        }
        if (!reverseResult.success()) {
            expectedBalance = expectedBalance.subtract(Money.of(1_000_000L));
        }
        assertEquals(expectedBalance, after.getPrincipalBalance(),
                "취소=" + reverseResult + " 상환=" + repayResult);
    }
}
