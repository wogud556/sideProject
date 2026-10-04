package com.hanati.bank.refinance.refinance;

import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.gateway.DemoScenarioAccounts;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.operator.entity.Operator;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.repository.OperatorRepository;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.domain.TransactionType;
import com.hanati.bank.refinance.refinance.dto.ApprovalRequest;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplyRequest;
import com.hanati.bank.refinance.refinance.repository.RefinanceTransactionRepository;
import com.hanati.bank.refinance.refinance.service.RefinanceApplicationService;
import com.hanati.bank.refinance.refinance.service.RefinanceApprovalService;
import com.hanati.bank.refinance.refinance.service.RefinanceExecutionService;
import com.hanati.bank.refinance.refinance.service.RefinanceRetryService;
import com.hanati.bank.refinance.refinance.service.RefinanceReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 명세 34번이 명시적으로 요구하는 핵심 시나리오:
 * 신규대출 실행 성공 -> 기존대출 상환 실패 -> 재처리 -> 기존대출 상환 성공 -> COMPLETED.
 * 이 과정에서 신규대출이 두 번 실행되어서는 안 된다 (TB_REFINANCE_TRANSACTION의
 * NEW_LOAN_EXECUTION 타입 거래가 정확히 1건이어야 함).
 */
@SpringBootTest
@Transactional
class RefinanceRetryFlowTest {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ExistingLoanRepository existingLoanRepository;
    @Autowired
    private OperatorRepository operatorRepository;
    @Autowired
    private RefinanceTransactionRepository refinanceTransactionRepository;
    @Autowired
    private RefinanceApplicationService refinanceApplicationService;
    @Autowired
    private RefinanceReviewService refinanceReviewService;
    @Autowired
    private RefinanceApprovalService refinanceApprovalService;
    @Autowired
    private RefinanceExecutionService refinanceExecutionService;
    @Autowired
    private RefinanceRetryService refinanceRetryService;

    @Test
    void recovers_via_retry_without_re_executing_the_new_loan() {
        seedOperatorsIfMissing();

        Customer customer = customerRepository.save(Customer.builder()
                .customerNo("T0002").name("재처리고객").birthDate(LocalDate.of(1988, 5, 5))
                .phone("010-0000-0002").status("ACTIVE").build());

        ExistingLoan loan = existingLoanRepository.save(ExistingLoan.builder()
                .customerId(customer.getCustomerId())
                .financialInstitutionCode("081").financialInstitutionName("하나은행")
                .loanAccountNo(DemoScenarioAccounts.REPAYMENT_FAILURE_ACCOUNT)
                .loanProductCode("081-P01").loanProductName("하나 신용대출").loanType("신용대출")
                .originalAmount(new BigDecimal("30000000")).currentBalance(new BigDecimal("20000000"))
                .interestRate(new BigDecimal("6.0"))
                .executionDate(LocalDate.now().minusYears(1)).maturityDate(LocalDate.now().plusYears(2))
                .repaymentMethod("원리금균등분할상환").overdueYn("N").status("ACTIVE")
                .build());

        RefinanceApplyRequest applyRequest = new RefinanceApplyRequest();
        applyRequest.setCustomerId(customer.getCustomerId());
        applyRequest.setLoanIds(List.of(loan.getLoanId()));
        applyRequest.setNewLoanProductName("가계대환신용대출");
        applyRequest.setNewLoanAmount(new BigDecimal("21000000"));
        applyRequest.setNewLoanRate(new BigDecimal("5.0"));
        applyRequest.setNewLoanRateType("고정금리");
        applyRequest.setNewLoanPeriodMonths(36);
        applyRequest.setNewLoanRepaymentMethod("원리금균등분할상환");
        applyRequest.setNewLoanExecutionScheduledDate(LocalDate.now());
        applyRequest.setNewLoanAccountNo("999-0000-0002");
        applyRequest.setRefinancePurposeYn("Y");

        RefinanceApplicationResponse applied = refinanceApplicationService.apply(applyRequest, "teller01");
        refinanceReviewService.review(applied.getApplicationId(), "이상 없음", "reviewer01");
        refinanceApprovalService.approve(applied.getApplicationId(), new ApprovalRequest(), "approver01");

        RefinanceApplicationResponse afterExecute = refinanceExecutionService.execute(applied.getApplicationId(), "operator01");
        assertEquals(RefinanceStatus.FAILED, afterExecute.getStatus());
        assertEquals(1, countTransactionsByType(applied.getApplicationId(), TransactionType.NEW_LOAN_EXECUTION),
                "신규대출 실행은 최초 1회만 발생해야 한다");

        RefinanceApplicationResponse afterRetry = refinanceRetryService.retry(applied.getApplicationId(), "operator01");
        assertEquals(RefinanceStatus.COMPLETED, afterRetry.getStatus());

        assertEquals(1, countTransactionsByType(applied.getApplicationId(), TransactionType.NEW_LOAN_EXECUTION),
                "재처리 이후에도 신규대출 실행 거래는 여전히 1건이어야 한다 (중복 실행 금지)");
        assertEquals(2, countTransactionsByType(applied.getApplicationId(), TransactionType.EXISTING_LOAN_REPAYMENT),
                "기존대출 상환은 최초 실패 1건 + 재처리 성공 1건, 총 2건이어야 한다");
    }

    private long countTransactionsByType(Long applicationId, TransactionType type) {
        return refinanceTransactionRepository.findByApplicationIdAndTransactionType(applicationId, type).size();
    }

    private void seedOperatorsIfMissing() {
        saveIfMissing("teller01", "창구직원", OperatorRole.ROLE_TELLER);
        saveIfMissing("reviewer01", "심사역", OperatorRole.ROLE_REVIEWER);
        saveIfMissing("approver01", "승인권자", OperatorRole.ROLE_APPROVER);
        saveIfMissing("operator01", "실행운영자", OperatorRole.ROLE_OPERATOR);
    }

    private void saveIfMissing(String id, String name, OperatorRole role) {
        if (operatorRepository.existsById(id)) return;
        operatorRepository.save(Operator.builder().operatorId(id).name(name).role(role).build());
    }
}
