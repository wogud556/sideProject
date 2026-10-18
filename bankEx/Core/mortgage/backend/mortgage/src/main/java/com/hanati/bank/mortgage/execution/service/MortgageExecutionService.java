package com.hanati.bank.mortgage.execution.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.approval.entity.MortgageAgreement;
import com.hanati.bank.mortgage.approval.entity.MortgageApproval;
import com.hanati.bank.mortgage.approval.service.MortgageAgreementService;
import com.hanati.bank.mortgage.approval.service.MortgageApprovalService;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.common.util.MortgageNoGenerator;
import com.hanati.bank.mortgage.execution.entity.MortgageExecution;
import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import com.hanati.bank.mortgage.execution.enums.ExecutionStatus;
import com.hanati.bank.mortgage.execution.enums.LoanAccountStatus;
import com.hanati.bank.mortgage.execution.repository.MortgageExecutionRepository;
import com.hanati.bank.mortgage.execution.repository.MortgageLoanAccountRepository;
import com.hanati.bank.mortgage.gateway.DisbursementFailedException;
import com.hanati.bank.mortgage.gateway.DisbursementGateway;
import com.hanati.bank.mortgage.gateway.DisbursementGateway.DisbursementCommand;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.repayment.service.RepaymentScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MortgageExecutionService {

    private final MortgageApplicationLoader loader;
    private final MortgageApprovalService approvalService;
    private final MortgageAgreementService agreementService;
    private final MortgageExecutionRepository executionRepository;
    private final MortgageLoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleService scheduleService;
    private final DisbursementGateway disbursementGateway;
    private final OperatorGuard operatorGuard;

    /**
     * 대출 실행 (명세 7): 선행조건 검증 → 실행금 지급 → 대출 계좌 생성 → 잔액 반영 → 상환 스케줄 생성.
     *
     * 정합성 장치
     * - 신청 행 비관적 잠금: 같은 건의 동시 실행을 한 줄로 세운다.
     * - idempotencyKey: 같은 요청의 재전송(더블클릭·타임아웃 재시도)은 처음 결과를 돌려준다.
     * - bankEx 입금 키를 신청 건마다 하나로 고정: 입금 후 이 트랜잭션이 롤백되어 다시 실행해도 입금은 한 번뿐이다.
     * - 입금 실패는 FAILED 이력만 남기고 실행 대기 상태를 유지한다. 새 키로 재시도하면 된다.
     */
    @Transactional
    public MortgageExecution execute(Long applicationId, String operatorId, String idempotencyKey, LocalDate today) {
        operatorGuard.require(operatorId, OperatorRole.OPERATIONS);
        MortgageApplication application = loader.lock(applicationId);

        Optional<MortgageExecution> replay = executionRepository.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            if (!replay.get().getApplicationId().equals(applicationId)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            return replay.get();
        }
        if (application.getStatus() == ApplicationStatus.EXECUTED) {
            throw new BusinessException(ErrorCode.ALREADY_EXECUTED);
        }
        if (application.getStatus() == ApplicationStatus.AGREED) {
            throw new BusinessException(ErrorCode.EXECUTION_PRECONDITION_NOT_MET);
        }
        application.requireTransitionTo(ApplicationStatus.EXECUTED);

        MortgageApproval approval = approvalService.get(applicationId);
        if (approval.isExpiredOn(today)) {
            throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
        }
        MortgageAgreement agreement = agreementService.get(applicationId);
        if (!agreement.isLienRegistered() || !approval.getConditionsFulfilled()) {
            throw new BusinessException(ErrorCode.EXECUTION_PRECONDITION_NOT_MET);
        }

        LocalDateTime now = LocalDateTime.now();
        MortgageExecution.MortgageExecutionBuilder execution = MortgageExecution.builder()
                .applicationId(applicationId)
                .idempotencyKey(idempotencyKey)
                .amount(agreement.getAgreedAmount())
                .disbursementAccountNo(agreement.getDisbursementAccountNo())
                .executedBy(operatorId)
                .executedAt(now);

        String bankTransactionNo;
        try {
            bankTransactionNo = disbursementGateway.disburse(new DisbursementCommand(
                    MortgageNoGenerator.disbursementKey(application.getApplicationNo()),
                    agreement.getDisbursementAccountNo(), agreement.getAgreedAmount(),
                    "주택담보대출 실행 " + application.getApplicationNo()));
        } catch (DisbursementFailedException e) {
            return saveWithNumber(execution.status(ExecutionStatus.FAILED).failureReason(e.getMessage()).build(), today);
        }

        MortgageLoanAccount account = loanAccountRepository.save(MortgageLoanAccount.builder()
                .applicationId(applicationId)
                .customerId(application.getCustomerId())
                .productCode(application.getProductCode())
                .principal(agreement.getAgreedAmount())
                .outstandingPrincipal(agreement.getAgreedAmount())
                .interestRate(agreement.getInterestRate())
                .termMonths(agreement.getTermMonths())
                .repaymentMethod(agreement.getRepaymentMethod())
                .executionDate(today)
                .maturityDate(today.plusMonths(agreement.getTermMonths()))
                .status(LoanAccountStatus.ACTIVE)
                .createdAt(now)
                .build());
        account.assignLoanAccountNo(MortgageNoGenerator.loanAccountNo(account.getLoanAccountId()));
        scheduleService.createSchedule(account);

        application.changeStatus(ApplicationStatus.EXECUTED, now);
        return saveWithNumber(execution.status(ExecutionStatus.COMPLETED)
                .bankTransactionNo(bankTransactionNo)
                .loanAccountId(account.getLoanAccountId())
                .build(), today);
    }

    private MortgageExecution saveWithNumber(MortgageExecution execution, LocalDate today) {
        MortgageExecution saved = executionRepository.save(execution);
        saved.assignExecutionNo(MortgageNoGenerator.executionNo(today, saved.getExecutionId()));
        return saved;
    }
}
