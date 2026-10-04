package com.hanati.bank.refinance.refinance;

import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.operator.entity.Operator;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.repository.OperatorRepository;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.ApprovalRequest;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplyRequest;
import com.hanati.bank.refinance.refinance.service.RefinanceApplicationService;
import com.hanati.bank.refinance.refinance.service.RefinanceApprovalService;
import com.hanati.bank.refinance.refinance.service.RefinanceExecutionService;
import com.hanati.bank.refinance.refinance.service.RefinanceReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 정상 시나리오 End-to-End: 신청 -> 심사 -> 승인 -> 실행(신규대출실행+기존대출상환) -> COMPLETED.
 * 실행 완료 후 동일 신청을 다시 실행 요청하면 차단되는지(중복 실행 방지)도 함께 검증한다 (명세 33/34번).
 */
@SpringBootTest
@Transactional
class RefinanceHappyPathFlowTest {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ExistingLoanRepository existingLoanRepository;
    @Autowired
    private OperatorRepository operatorRepository;
    @Autowired
    private RefinanceApplicationService refinanceApplicationService;
    @Autowired
    private RefinanceReviewService refinanceReviewService;
    @Autowired
    private RefinanceApprovalService refinanceApprovalService;
    @Autowired
    private RefinanceExecutionService refinanceExecutionService;

    @Test
    void completes_the_full_refinance_flow_and_blocks_duplicate_execution() {
        seedOperatorsIfMissing();

        Customer customer = customerRepository.save(Customer.builder()
                .customerNo("T0001").name("테스트고객").birthDate(LocalDate.of(1990, 1, 1))
                .phone("010-0000-0001").status("ACTIVE").build());

        ExistingLoan loan = existingLoanRepository.save(ExistingLoan.builder()
                .customerId(customer.getCustomerId())
                .financialInstitutionCode("004").financialInstitutionName("국민은행")
                .loanAccountNo("111-1111-9999").loanProductCode("004-P01").loanProductName("KB 신용대출")
                .loanType("신용대출")
                .originalAmount(new BigDecimal("20000000")).currentBalance(new BigDecimal("15000000"))
                .interestRate(new BigDecimal("6.0"))
                .executionDate(LocalDate.now().minusYears(1)).maturityDate(LocalDate.now().plusYears(2))
                .repaymentMethod("원리금균등분할상환").overdueYn("N").status("ACTIVE")
                .build());

        RefinanceApplyRequest applyRequest = new RefinanceApplyRequest();
        applyRequest.setCustomerId(customer.getCustomerId());
        applyRequest.setLoanIds(List.of(loan.getLoanId()));
        applyRequest.setNewLoanProductName("가계대환신용대출");
        applyRequest.setNewLoanAmount(new BigDecimal("16000000"));
        applyRequest.setNewLoanRate(new BigDecimal("5.0"));
        applyRequest.setNewLoanRateType("고정금리");
        applyRequest.setNewLoanPeriodMonths(36);
        applyRequest.setNewLoanRepaymentMethod("원리금균등분할상환");
        applyRequest.setNewLoanExecutionScheduledDate(LocalDate.now());
        applyRequest.setNewLoanAccountNo("999-0000-0001");
        applyRequest.setRefinancePurposeYn("Y");

        RefinanceApplicationResponse applied = refinanceApplicationService.apply(applyRequest, "teller01");
        assertEquals(RefinanceStatus.REQUESTED, applied.getStatus());

        RefinanceApplicationResponse reviewed = refinanceReviewService.review(applied.getApplicationId(), "이상 없음", "reviewer01");
        assertEquals(RefinanceStatus.REVIEWING, reviewed.getStatus());

        RefinanceApplicationResponse approved = refinanceApprovalService.approve(applied.getApplicationId(), new ApprovalRequest(), "approver01");
        assertEquals(RefinanceStatus.APPROVED, approved.getStatus());

        RefinanceApplicationResponse executed = refinanceExecutionService.execute(applied.getApplicationId(), "operator01");
        assertEquals(RefinanceStatus.COMPLETED, executed.getStatus());

        assertThrows(BusinessException.class,
                () -> refinanceExecutionService.execute(applied.getApplicationId(), "operator01"));
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
