package com.hanati.bank.mortgage.flow;

import com.hanati.bank.mortgage.application.dto.ApplicationCreateRequest;
import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.enums.*;
import com.hanati.bank.mortgage.application.service.MortgageApplicationQueryService;
import com.hanati.bank.mortgage.application.service.MortgageApplicationService;
import com.hanati.bank.mortgage.approval.dto.AgreementRequest;
import com.hanati.bank.mortgage.approval.dto.ApprovalRequest;
import com.hanati.bank.mortgage.approval.dto.PreconditionRequest;
import com.hanati.bank.mortgage.approval.service.MortgageAgreementService;
import com.hanati.bank.mortgage.approval.service.MortgageApprovalService;
import com.hanati.bank.mortgage.collateral.dto.CollateralEvaluationRequest;
import com.hanati.bank.mortgage.collateral.enums.LienType;
import com.hanati.bank.mortgage.collateral.enums.ValuationMethod;
import com.hanati.bank.mortgage.collateral.service.CollateralService;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.execution.entity.MortgageExecution;
import com.hanati.bank.mortgage.execution.enums.ExecutionStatus;
import com.hanati.bank.mortgage.execution.enums.LoanAccountStatus;
import com.hanati.bank.mortgage.execution.service.MortgageExecutionService;
import com.hanati.bank.mortgage.gateway.DisbursementFailedException;
import com.hanati.bank.mortgage.gateway.DisbursementGateway;
import com.hanati.bank.mortgage.gateway.DisbursementGateway.DisbursementCommand;
import com.hanati.bank.mortgage.limit.service.LimitCalculationRequest;
import com.hanati.bank.mortgage.limit.service.MortgageLimitService;
import com.hanati.bank.mortgage.repayment.dto.LoanAccountResponse;
import com.hanati.bank.mortgage.repayment.dto.RepaymentRequest;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentHistory;
import com.hanati.bank.mortgage.repayment.entity.MortgageRepaymentSchedule;
import com.hanati.bank.mortgage.repayment.service.MortgageRepaymentService;
import com.hanati.bank.mortgage.screening.service.MortgageScreeningService;
import com.hanati.bank.mortgage.screening.service.ScreeningRequest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class MortgageFlowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final String CUSTOMER = "cust01";
    private static final String ACCOUNT = "110-123-456789";

    @Autowired MortgageApplicationService applicationService;
    @Autowired MortgageApplicationQueryService queryService;
    @Autowired CollateralService collateralService;
    @Autowired MortgageLimitService limitService;
    @Autowired MortgageScreeningService screeningService;
    @Autowired MortgageApprovalService approvalService;
    @Autowired MortgageAgreementService agreementService;
    @Autowired MortgageExecutionService executionService;
    @Autowired MortgageRepaymentService repaymentService;

    @MockitoBean DisbursementGateway gateway;

    @BeforeEach
    void setUp() {
        when(gateway.disburse(any())).thenReturn("TXN-BANKEX-1");
    }

    // ── 시나리오 도우미 ──

    /** 비규제지역 무주택자, 5억 주택, 연소득 6,500만, 3억 신청 (명세 예시와 같은 조건) */
    private Long apply(long amount, RegionCode region, int houseCount) {
        return applicationService.apply(new ApplicationCreateRequest(CUSTOMER, "홍길동", "MG-STD", amount, 360,
                RepaymentMethod.EQUAL_INSTALLMENT, LoanPurpose.PURCHASE, houseCount, false,
                new ApplicationCreateRequest.Property("서울시 테스트구", region, PropertyType.APARTMENT,
                        new BigDecimal("84.00"), 500_000_000L),
                new ApplicationCreateRequest.Income(IncomeType.EMPLOYEE, 65_000_000L, "테스트㈜"),
                List.of()), TODAY).getApplicationId();
    }

    private Long apply() {
        return apply(300_000_000L, RegionCode.NON_REGULATED, 0);
    }

    private void evaluateAndLimit(Long id) {
        collateralService.evaluate(id, "OPS01", new CollateralEvaluationRequest(ValuationMethod.MARKET_PRICE,
                500_000_000L, List.of()), TODAY);
        limitService.calculate(id, "UW01", new LimitCalculationRequest(850), TODAY);
    }

    private Long readyToExecute() {
        Long id = apply();
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY);
        agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY);
        agreementService.confirmPreconditions(id, "OPS01", new PreconditionRequest(false));
        return id;
    }

    private ApplicationStatus status(Long id) {
        return queryService.getDetail(id).application().status();
    }

    private static void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).extracting("errorCode").isEqualTo(code);
    }

    // ── 정상 흐름 ──

    @Test
    @DisplayName("신청부터 실행·첫 회차 상환까지 단계별 상태가 이어진다")
    void happyPath() {
        Long id = apply();
        assertThat(status(id)).isEqualTo(ApplicationStatus.APPLIED);

        evaluateAndLimit(id);
        ApplicationDetailResponse limited = queryService.getDetail(id);
        assertThat(limited.application().status()).isEqualTo(ApplicationStatus.LIMIT_CALCULATED);
        assertThat(limited.limit().getFinalLimit()).isEqualTo(350_000_000L);
        assertThat(limited.limit().getAppliedRate()).isEqualByComparingTo("4.10");
        assertThat(limited.limit().getStressRate()).isEqualByComparingTo("1.50");

        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        assertThat(status(id)).isEqualTo(ApplicationStatus.SCREENING_APPROVED);

        approvalService.approve(id, "AP01", new ApprovalRequest(null, "정상 승인"), TODAY);
        assertThat(status(id)).isEqualTo(ApplicationStatus.APPROVED);

        agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY);
        assertThat(status(id)).isEqualTo(ApplicationStatus.AGREED);

        agreementService.confirmPreconditions(id, "OPS01", new PreconditionRequest(false));
        ApplicationDetailResponse ready = queryService.getDetail(id);
        assertThat(ready.application().status()).isEqualTo(ApplicationStatus.EXECUTION_READY);
        assertThat(ready.liens()).anySatisfy(l -> {
            assertThat(l.getLienType()).isEqualTo(LienType.OWN_MORTGAGE);
            assertThat(l.getAmount()).isEqualTo(360_000_000L); // 3억 × 120%
        });

        MortgageExecution execution = executionService.execute(id, "OPS01", "key-1", TODAY);
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(execution.getBankTransactionNo()).isEqualTo("TXN-BANKEX-1");
        assertThat(status(id)).isEqualTo(ApplicationStatus.EXECUTED);

        ArgumentCaptor<DisbursementCommand> cmd = ArgumentCaptor.forClass(DisbursementCommand.class);
        verify(gateway).disburse(cmd.capture());
        assertThat(cmd.getValue().accountNo()).isEqualTo(ACCOUNT);
        assertThat(cmd.getValue().amount()).isEqualTo(300_000_000L);

        Long accountId = execution.getLoanAccountId();
        List<MortgageRepaymentSchedule> schedule = repaymentService.getSchedule(accountId);
        assertThat(schedule).hasSize(360);
        assertThat(schedule.get(0).getDueDate()).isEqualTo(TODAY.plusMonths(1));

        long due = schedule.get(0).getTotalPayment();
        MortgageRepaymentHistory paid = repaymentService.repay(accountId,
                new RepaymentRequest(CUSTOMER, due, ACCOUNT, "TXN-REPAY-1"));
        assertThat(paid.getInstallmentNo()).isEqualTo(1);
        LoanAccountResponse account = repaymentService.getAccount(accountId);
        assertThat(account.account().getOutstandingPrincipal()).isEqualTo(300_000_000L - schedule.get(0).getPrincipal());
        assertThat(account.nextInstallment().getInstallmentNo()).isEqualTo(2);
    }

    // ── 순서·권한 ──

    @Test
    @DisplayName("단계를 건너뛸 수 없다")
    void cannotSkipSteps() {
        Long id = apply();
        assertError(() -> limitService.calculate(id, "UW01", new LimitCalculationRequest(850), TODAY),
                ErrorCode.INVALID_STATUS_TRANSITION);
        assertError(() -> screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null)),
                ErrorCode.INVALID_STATUS_TRANSITION);
        assertError(() -> approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY),
                ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("담보 재평가는 한도 산정 결과를 무효로 돌려 재산정을 요구한다")
    void reEvaluationRequiresRecalculation() {
        Long id = apply();
        evaluateAndLimit(id);
        collateralService.evaluate(id, "OPS01", new CollateralEvaluationRequest(ValuationMethod.APPRAISAL,
                450_000_000L, List.of()), TODAY);

        assertThat(status(id)).isEqualTo(ApplicationStatus.COLLATERAL_EVALUATED);
        assertThat(queryService.getDetail(id).valuations()).hasSize(2);
        assertError(() -> screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null)),
                ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("업무마다 정해진 역할만 처리할 수 있다 — 심사역은 승인할 수 없다")
    void roleSeparation() {
        Long id = apply();
        assertError(() -> collateralService.evaluate(id, "UW01", new CollateralEvaluationRequest(
                ValuationMethod.MARKET_PRICE, 500_000_000L, List.of()), TODAY), ErrorCode.OPERATOR_NOT_AUTHORIZED);

        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        assertError(() -> approvalService.approve(id, "UW01", new ApprovalRequest(null, null), TODAY),
                ErrorCode.OPERATOR_NOT_AUTHORIZED);
        assertError(() -> approvalService.approve(id, "NOBODY", new ApprovalRequest(null, null), TODAY),
                ErrorCode.OPERATOR_NOT_FOUND);
    }

    // ── 심사 결과별 ──

    @Test
    @DisplayName("규제지역 다주택자는 LTV 0%로 한도가 없어 거절되고, 이후 단계는 막힌다")
    void multiHomeInRegulatedAreaRejected() {
        Long id = apply(300_000_000L, RegionCode.REGULATED, 2);
        evaluateAndLimit(id);
        assertThat(queryService.getDetail(id).limit().getFinalLimit()).isZero();

        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        assertThat(status(id)).isEqualTo(ApplicationStatus.REJECTED);
        assertError(() -> approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY),
                ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("소득 증빙이 없으면 추가 서류 요청 후 재심사할 수 있다")
    void additionalDocuments() {
        Long id = apply();
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(false, true, null));
        assertThat(status(id)).isEqualTo(ApplicationStatus.DOCUMENT_REQUIRED);

        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, "원천징수영수증 확인"));
        assertThat(status(id)).isEqualTo(ApplicationStatus.SCREENING_APPROVED);
        assertThat(queryService.getDetail(id).screenings()).hasSize(2);
    }

    @Test
    @DisplayName("신청 금액이 한도를 넘으면 한도 금액으로 감액 승인하고, 그 이상은 승인할 수 없다")
    void reducedApproval() {
        Long id = apply(400_000_000L, RegionCode.NON_REGULATED, 0);
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));

        ApplicationDetailResponse detail = queryService.getDetail(id);
        assertThat(detail.application().status()).isEqualTo(ApplicationStatus.CONDITIONALLY_APPROVED);
        assertThat(detail.screenings().get(0).recommendedAmount()).isEqualTo(350_000_000L);

        assertError(() -> approvalService.approve(id, "AP01", new ApprovalRequest(350_010_000L, null), TODAY),
                ErrorCode.INVALID_APPROVED_AMOUNT);
        assertThat(approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY).getApprovedAmount())
                .isEqualTo(350_000_000L);
    }

    @Test
    @DisplayName("조건부 승인 건은 조건 이행을 확인해야 실행 대기가 된다")
    void conditionalApprovalNeedsFulfilment() {
        Long id = apply();
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, false, null));
        approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY);
        assertThat(approvalService.get(id).getConditions()).contains("소유권");
        agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY);

        agreementService.confirmPreconditions(id, "OPS01", new PreconditionRequest(false));
        assertThat(status(id)).isEqualTo(ApplicationStatus.AGREED);
        assertThat(agreementService.get(id).isLienRegistered()).isTrue();
        assertError(() -> executionService.execute(id, "OPS01", "key-c", TODAY),
                ErrorCode.EXECUTION_PRECONDITION_NOT_MET);

        agreementService.confirmPreconditions(id, "OPS01", new PreconditionRequest(true));
        assertThat(status(id)).isEqualTo(ApplicationStatus.EXECUTION_READY);
        // 근저당은 한 번만 설정된다
        assertThat(queryService.getDetail(id).liens()).filteredOn(l -> l.getLienType() == LienType.OWN_MORTGAGE)
                .hasSize(1);
    }

    // ── 고객 요청 ──

    @Test
    @DisplayName("다른 고객은 약정·철회할 수 없고, 동의 없이는 약정할 수 없다")
    void customerOwnership() {
        Long id = apply();
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY);

        assertError(() -> agreementService.agree(id, new AgreementRequest("other", ACCOUNT, true), TODAY),
                ErrorCode.APPLICATION_NOT_OWNED);
        assertError(() -> applicationService.withdraw(id, "other"), ErrorCode.APPLICATION_NOT_OWNED);
        assertError(() -> agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, false), TODAY),
                ErrorCode.AGREEMENT_CONSENT_REQUIRED);

        applicationService.withdraw(id, CUSTOMER);
        assertThat(status(id)).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertError(() -> agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY),
                ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("승인 유효기간(30일)이 지나면 약정할 수 없다")
    void approvalExpiry() {
        Long id = apply();
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY);

        assertError(() -> agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY.plusDays(30)),
                ErrorCode.APPROVAL_EXPIRED);
        agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY.plusDays(29));
        assertThat(status(id)).isEqualTo(ApplicationStatus.AGREED);
    }

    @Test
    @DisplayName("신청 금액·기간이 상품 범위를 벗어나면 접수하지 않는다")
    void productRange() {
        assertError(() -> apply(1_000_000_001L, RegionCode.NON_REGULATED, 0), ErrorCode.AMOUNT_OUT_OF_PRODUCT_RANGE);
    }

    // ── 실행 정합성 ──

    @Test
    @DisplayName("같은 요청 키로 다시 실행하면 처음 결과를 돌려주고 입금은 한 번뿐이다")
    void idempotentExecution() {
        Long id = readyToExecute();

        MortgageExecution first = executionService.execute(id, "OPS01", "key-same", TODAY);
        MortgageExecution second = executionService.execute(id, "OPS01", "key-same", TODAY);

        assertThat(second.getExecutionId()).isEqualTo(first.getExecutionId());
        verify(gateway, times(1)).disburse(any());
        assertError(() -> executionService.execute(id, "OPS01", "key-other", TODAY), ErrorCode.ALREADY_EXECUTED);
    }

    @Test
    @DisplayName("입금 실패는 FAILED 이력만 남기고 실행 대기를 유지하며, 재시도는 같은 입금 키를 쓴다")
    void failedDisbursementCanBeRetried() {
        Long id = readyToExecute();
        when(gateway.disburse(any()))
                .thenThrow(new DisbursementFailedException("bankEx에 연결할 수 없습니다."))
                .thenReturn("TXN-BANKEX-2");

        MortgageExecution failed = executionService.execute(id, "OPS01", "key-fail", TODAY);
        assertThat(failed.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(failed.getFailureReason()).contains("연결");
        assertThat(failed.getLoanAccountId()).isNull();
        assertThat(status(id)).isEqualTo(ApplicationStatus.EXECUTION_READY);

        MortgageExecution retried = executionService.execute(id, "OPS01", "key-retry", TODAY);
        assertThat(retried.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(queryService.getDetail(id).executions()).hasSize(2);

        ArgumentCaptor<DisbursementCommand> cmd = ArgumentCaptor.forClass(DisbursementCommand.class);
        verify(gateway, times(2)).disburse(cmd.capture());
        assertThat(cmd.getAllValues().get(0).idempotencyKey()).isEqualTo(cmd.getAllValues().get(1).idempotencyKey());
    }

    // ── 상환 ──

    @Test
    @DisplayName("상환: 청구액과 다르면 거부, 같은 거래번호는 한 번만 반영, 본인 계좌만")
    void repaymentGuards() {
        Long id = readyToExecute();
        Long accountId = executionService.execute(id, "OPS01", "key-r", TODAY).getLoanAccountId();
        long due = repaymentService.getAccount(accountId).nextInstallment().getTotalPayment();

        assertError(() -> repaymentService.repay(accountId, new RepaymentRequest(CUSTOMER, due - 1, ACCOUNT, "T1")),
                ErrorCode.REPAYMENT_AMOUNT_MISMATCH);
        assertError(() -> repaymentService.repay(accountId, new RepaymentRequest("other", due, ACCOUNT, "T1")),
                ErrorCode.LOAN_ACCOUNT_NOT_OWNED);

        MortgageRepaymentHistory first = repaymentService.repay(accountId, new RepaymentRequest(CUSTOMER, due, ACCOUNT, "T1"));
        MortgageRepaymentHistory replay = repaymentService.repay(accountId, new RepaymentRequest(CUSTOMER, due, ACCOUNT, "T1"));
        assertThat(replay.getRepaymentId()).isEqualTo(first.getRepaymentId());
        assertThat(repaymentService.getHistory(accountId)).hasSize(1);
    }

    @Test
    @DisplayName("마지막 회차까지 갚으면 대출 계좌가 완제된다")
    void fullyRepaidClosesAccount() {
        Long id = apply(30_000_000L, RegionCode.NON_REGULATED, 0);
        evaluateAndLimit(id);
        screeningService.screen(id, "UW01", new ScreeningRequest(true, true, null));
        approvalService.approve(id, "AP01", new ApprovalRequest(null, null), TODAY);
        agreementService.agree(id, new AgreementRequest(CUSTOMER, ACCOUNT, true), TODAY);
        agreementService.confirmPreconditions(id, "OPS01", new PreconditionRequest(false));
        Long accountId = executionService.execute(id, "OPS01", "key-full", TODAY).getLoanAccountId();

        for (MortgageRepaymentSchedule s : repaymentService.getSchedule(accountId)) {
            repaymentService.repay(accountId, new RepaymentRequest(CUSTOMER, s.getTotalPayment(), ACCOUNT,
                    "T-" + s.getInstallmentNo()));
        }

        LoanAccountResponse account = repaymentService.getAccount(accountId);
        assertThat(account.account().getStatus()).isEqualTo(LoanAccountStatus.CLOSED);
        assertThat(account.account().getOutstandingPrincipal()).isZero();
        assertThat(account.nextInstallment()).isNull();
        assertError(() -> repaymentService.repay(accountId, new RepaymentRequest(CUSTOMER, 1L, ACCOUNT, "T-after")),
                ErrorCode.LOAN_ACCOUNT_CLOSED);
    }
}
