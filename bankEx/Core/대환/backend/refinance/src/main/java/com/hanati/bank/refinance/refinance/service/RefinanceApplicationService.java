package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.common.util.RefinanceApplicationNoGenerator;
import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.refinance.calculator.RepaymentAmountCalculator;
import com.hanati.bank.refinance.refinance.calculator.RepaymentAmountResult;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.EligibilityResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplyRequest;
import com.hanati.bank.refinance.refinance.dto.RefinanceTargetResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import com.hanati.bank.refinance.refinance.entity.RefinanceTarget;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceApplicationService {

    private final RefinanceApplicationRepository refinanceApplicationRepository;
    private final RefinanceTargetRepository refinanceTargetRepository;
    private final RefinanceHistoryRepository refinanceHistoryRepository;
    private final CustomerRepository customerRepository;
    private final ExistingLoanRepository existingLoanRepository;
    private final RefinanceEligibilityService refinanceEligibilityService;
    private final RepaymentAmountCalculator repaymentAmountCalculator;
    private final AuditLogService auditLogService;

    @Transactional
    public RefinanceApplicationResponse apply(RefinanceApplyRequest request, String operatorId) {
        validateRequest(request);

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // Frontend에서 이미 검증했더라도 서버가 다시 한번 검증한다 (명세 30번).
        EligibilityResponse eligibility = refinanceEligibilityService.check(request.getCustomerId(), request.getLoanIds());
        if (!eligibility.eligible()) {
            throw new BusinessException(ErrorCode.REFINANCE_NOT_ELIGIBLE);
        }

        List<ExistingLoan> loans = request.getLoanIds().stream()
                .map(id -> existingLoanRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND)))
                .toList();

        List<RepaymentAmountResult> repaymentResults = loans.stream()
                .map(loan -> repaymentAmountCalculator.calculate(loan, LocalDate.now()))
                .toList();

        BigDecimal totalRepaymentAmount = repaymentResults.stream()
                .map(RepaymentAmountResult::finalRepaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 신규대출금액과 대환 대상 금액의 관계를 검증한다 (명세 10번): 신규대출로 기존대출 전액을 상환할 수 있어야 한다.
        if (request.getNewLoanAmount().compareTo(totalRepaymentAmount) < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        String applicationNo = RefinanceApplicationNoGenerator.generate(refinanceApplicationRepository::existsByApplicationNo);
        LocalDateTime now = LocalDateTime.now();

        RefinanceApplication application = RefinanceApplication.builder()
                .applicationNo(applicationNo)
                .customerId(customer.getCustomerId())
                .applicationDate(now)
                .status(RefinanceStatus.REQUESTED)
                .requestedAmount(request.getNewLoanAmount())
                .newLoanProductName(request.getNewLoanProductName())
                .newLoanAmount(request.getNewLoanAmount())
                .newLoanRate(request.getNewLoanRate())
                .newLoanRateType(request.getNewLoanRateType())
                .newLoanPeriodMonths(request.getNewLoanPeriodMonths())
                .newLoanMaturityDate(request.getNewLoanExecutionScheduledDate().plusMonths(request.getNewLoanPeriodMonths()))
                .newLoanRepaymentMethod(request.getNewLoanRepaymentMethod())
                .newLoanExecutionScheduledDate(request.getNewLoanExecutionScheduledDate())
                .newLoanAccountNo(request.getNewLoanAccountNo())
                .refinancePurposeYn(request.getRefinancePurposeYn())
                .createdBy(operatorId)
                .createdAt(now)
                .updatedBy(operatorId)
                .updatedAt(now)
                .build();
        refinanceApplicationRepository.save(application);

        for (int i = 0; i < loans.size(); i++) {
            ExistingLoan loan = loans.get(i);
            RepaymentAmountResult result = repaymentResults.get(i);
            refinanceTargetRepository.save(RefinanceTarget.builder()
                    .applicationId(application.getApplicationId())
                    .loanId(loan.getLoanId())
                    .financialInstitutionCode(loan.getFinancialInstitutionCode())
                    .loanAccountNo(loan.getLoanAccountNo())
                    .loanProductCode(loan.getLoanProductCode())
                    .loanBalance(loan.getCurrentBalance())
                    .repaymentAmount(result.finalRepaymentAmount())
                    .prepaymentFee(result.prepaymentFee())
                    .interestAmount(result.accruedInterest())
                    .build());
        }

        refinanceHistoryRepository.save(RefinanceHistory.builder()
                .applicationId(application.getApplicationId())
                .actionType("신청등록")
                .fromStatus(null)
                .toStatus(RefinanceStatus.REQUESTED)
                .description(applicationNo + " 대환 신청이 등록되었습니다.")
                .processedBy(operatorId)
                .processedAt(now)
                .build());

        auditLogService.record(operatorId, "신청등록", customer.getCustomerId(), application.getApplicationId(),
                applicationNo + " 대환 신청 등록");

        return toResponse(application);
    }

    public List<RefinanceApplicationResponse> list() {
        return refinanceApplicationRepository.findAllByOrderByApplicationDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public RefinanceApplicationResponse get(Long applicationId) {
        return toResponse(getApplicationOrThrow(applicationId));
    }

    public RefinanceApplication getApplicationOrThrow(Long applicationId) {
        return refinanceApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    private void validateRequest(RefinanceApplyRequest request) {
        if (request.getCustomerId() == null || request.getLoanIds() == null || request.getLoanIds().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getNewLoanAmount() == null || request.getNewLoanAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (request.getNewLoanPeriodMonths() == null || request.getNewLoanPeriodMonths() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getNewLoanExecutionScheduledDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public RefinanceApplicationResponse toResponse(RefinanceApplication application) {
        List<RefinanceTargetResponse> targets = refinanceTargetRepository.findByApplicationId(application.getApplicationId())
                .stream().map(RefinanceTargetResponse::new).toList();
        return new RefinanceApplicationResponse(application, targets);
    }
}
