package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.gateway.LoanExecutionGateway;
import com.hanati.bank.refinance.gateway.LoanRepaymentGateway;
import com.hanati.bank.refinance.gateway.dto.LoanExecutionRequest;
import com.hanati.bank.refinance.gateway.dto.LoanExecutionResult;
import com.hanati.bank.refinance.gateway.dto.RepaymentRequest;
import com.hanati.bank.refinance.gateway.dto.RepaymentResult;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.service.OperatorAuthService;
import com.hanati.bank.refinance.refinance.domain.FailedStep;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.domain.TransactionType;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.entity.RefinanceError;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import com.hanati.bank.refinance.refinance.entity.RefinanceTarget;
import com.hanati.bank.refinance.refinance.entity.RefinanceTransaction;
import com.hanati.bank.refinance.refinance.domain.ErrorProcessStatus;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceErrorRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceTargetRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 대환 실행 오케스트레이션 (명세 14/15/16/17번).
 * 실행 순서: 현재상태확인 → 중복실행확인(상태조건부 UPDATE) → 신규대출실행 → 결과저장 → 기존대출상환 → 결과저장 → 완료처리.
 * 신규대출 실행 성공 후 기존대출 상환이 실패하면 DB Transaction을 롤백하지 않는다 — 이미 외부에서 신규대출이
 * 실행되었기 때문이다 (명세 17번). 대신 상태를 FAILED로 두고 TB_REFINANCE_ERROR에 실패 단계를 기록해
 * RefinanceRetryService가 실패한 단계부터만 재처리하도록 한다.
 */
@Service
@RequiredArgsConstructor
public class RefinanceExecutionService {

    private final RefinanceApplicationRepository refinanceApplicationRepository;
    private final RefinanceTargetRepository refinanceTargetRepository;
    private final RefinanceTransactionRepository refinanceTransactionRepository;
    private final RefinanceHistoryRepository refinanceHistoryRepository;
    final RefinanceErrorRepository refinanceErrorRepository;
    private final RefinanceApplicationService refinanceApplicationService;
    private final OperatorAuthService operatorAuthService;
    private final AuditLogService auditLogService;
    private final LoanExecutionGateway loanExecutionGateway;
    private final LoanRepaymentGateway loanRepaymentGateway;

    @Transactional
    public RefinanceApplicationResponse execute(Long applicationId, String operatorId) {
        operatorAuthService.requireRole(operatorId, OperatorRole.ROLE_OPERATOR);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        if (application.getStatus() != RefinanceStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
        }

        // 상태조건부 UPDATE: 이미 다른 요청이 먼저 EXECUTING으로 전이시켰다면 0건 갱신되어 중복 실행이 차단된다 (명세 22/33번).
        int updated = refinanceApplicationRepository.updateStatusIfMatch(applicationId, RefinanceStatus.APPROVED, RefinanceStatus.EXECUTING);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        recordHistory(applicationId, RefinanceStatus.APPROVED, RefinanceStatus.EXECUTING, "실행", "대환 실행을 시작합니다.", operatorId);

        boolean newLoanOk = executeNewLoanStep(application, operatorId);
        if (!newLoanOk) {
            RefinanceApplication failedState = refinanceApplicationService.getApplicationOrThrow(applicationId);
            auditLogService.record(operatorId, "실행", failedState.getCustomerId(), applicationId, "대환 실행 실패 (신규대출 실행 단계)");
            return refinanceApplicationService.toResponse(failedState);
        }

        transitionToRepaying(refinanceApplicationService.getApplicationOrThrow(applicationId), operatorId);
        boolean repaymentOk = repayExistingLoansStep(refinanceApplicationService.getApplicationOrThrow(applicationId), operatorId);
        RefinanceApplication finalState = refinanceApplicationService.getApplicationOrThrow(applicationId);

        if (repaymentOk) {
            auditLogService.record(operatorId, "실행", finalState.getCustomerId(), applicationId, "대환 실행 완료 (COMPLETED)");
        } else {
            auditLogService.record(operatorId, "실행", finalState.getCustomerId(), applicationId, "대환 실행 실패 (기존대출 상환 단계)");
        }
        return refinanceApplicationService.toResponse(finalState);
    }

    /**
     * 신규대출 실행 단계. RefinanceRetryService가 failedStep=NEW_LOAN_EXECUTION 재처리 시에도 재사용한다.
     */
    boolean executeNewLoanStep(RefinanceApplication application, String operatorId) {
        List<String> targetAccountNos = refinanceTargetRepository.findByApplicationId(application.getApplicationId())
                .stream().map(RefinanceTarget::getLoanAccountNo).toList();

        String requestId = "NLE-" + application.getApplicationId() + "-" + UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now();

        LoanExecutionRequest request = new LoanExecutionRequest(
                requestId, application.getApplicationId(), application.getCustomerId(),
                application.getNewLoanProductName(), application.getNewLoanAmount(),
                application.getNewLoanAccountNo(), targetAccountNos
        );
        LoanExecutionResult result = loanExecutionGateway.executeLoan(request);

        refinanceTransactionRepository.save(RefinanceTransaction.builder()
                .applicationId(application.getApplicationId())
                .transactionType(TransactionType.NEW_LOAN_EXECUTION)
                .requestId(requestId)
                .transactionNo(result.transactionNo())
                .requestData("신규대출 실행 요청: " + application.getNewLoanAmount() + "원")
                .responseData(result.resultMessage())
                .resultCode(result.resultCode())
                .resultMessage(result.resultMessage())
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .processedBy(operatorId)
                .build());

        if (!result.success()) {
            application.changeStatus(RefinanceStatus.FAILED, operatorId);
            refinanceApplicationRepository.save(application);
            markError(application, null, FailedStep.NEW_LOAN_EXECUTION, result.resultCode(), result.resultMessage());
            recordHistory(application.getApplicationId(), RefinanceStatus.EXECUTING, RefinanceStatus.FAILED,
                    "실행실패", "신규대출 실행 실패: " + result.resultMessage(), operatorId);
            return false;
        }

        application.changeStatus(RefinanceStatus.NEW_LOAN_EXECUTED, operatorId);
        refinanceApplicationRepository.save(application);
        recordHistory(application.getApplicationId(), RefinanceStatus.EXECUTING, RefinanceStatus.NEW_LOAN_EXECUTED,
                "신규대출실행", "신규대출 실행 완료 (거래번호 " + result.transactionNo() + ")", operatorId);
        return true;
    }

    /**
     * NEW_LOAN_EXECUTED -> REPAYING 전이만 담당한다. execute()의 정상 흐름과 RefinanceRetryService가
     * failedStep=NEW_LOAN_EXECUTION을 재처리해 신규대출 실행을 다시 성공시킨 직후 공통으로 호출한다.
     * failedStep=EXISTING_LOAN_REPAYMENT 재처리는 이미 REPAYING 상태이므로 이 메서드를 거치지 않는다.
     */
    void transitionToRepaying(RefinanceApplication application, String operatorId) {
        application.changeStatus(RefinanceStatus.REPAYING, operatorId);
        refinanceApplicationRepository.save(application);
        recordHistory(application.getApplicationId(), RefinanceStatus.NEW_LOAN_EXECUTED, RefinanceStatus.REPAYING,
                "상환시작", "기존대출 상환을 시작합니다.", operatorId);
    }

    /**
     * 기존대출 상환 단계. 호출 시점에 application은 이미 REPAYING 상태여야 한다.
     * RefinanceRetryService가 failedStep=EXISTING_LOAN_REPAYMENT 재처리 시에도 재사용하며,
     * 이 경우 신규대출 실행 단계는 절대 다시 호출하지 않는다 — 신규대출 중복 실행 방지의 핵심.
     */
    boolean repayExistingLoansStep(RefinanceApplication application, String operatorId) {
        List<RefinanceTarget> targets = refinanceTargetRepository.findByApplicationId(application.getApplicationId());

        for (RefinanceTarget target : targets) {
            String requestId = "ELR-" + application.getApplicationId() + "-" + target.getTargetId() + "-" + UUID.randomUUID();
            LocalDateTime startedAt = LocalDateTime.now();

            RepaymentRequest request = new RepaymentRequest(
                    requestId, application.getApplicationId(), target.getLoanId(),
                    target.getFinancialInstitutionCode(), target.getLoanAccountNo(), target.getRepaymentAmount()
            );
            RepaymentResult result = loanRepaymentGateway.repay(request);

            RefinanceTransaction savedTx = refinanceTransactionRepository.save(RefinanceTransaction.builder()
                    .applicationId(application.getApplicationId())
                    .transactionType(TransactionType.EXISTING_LOAN_REPAYMENT)
                    .requestId(requestId)
                    .transactionNo(result.transactionNo())
                    .requestData("기존대출 상환 요청: " + target.getRepaymentAmount() + "원 (targetId=" + target.getTargetId() + ")")
                    .responseData(result.resultMessage())
                    .resultCode(result.resultCode())
                    .resultMessage(result.resultMessage())
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .processedBy(operatorId)
                    .build());

            if (!result.success()) {
                application.changeStatus(RefinanceStatus.FAILED, operatorId);
                refinanceApplicationRepository.save(application);
                markError(application, savedTx.getTransactionId(), FailedStep.EXISTING_LOAN_REPAYMENT, result.resultCode(), result.resultMessage());
                recordHistory(application.getApplicationId(), RefinanceStatus.REPAYING, RefinanceStatus.FAILED,
                        "상환실패", "기존대출 상환 실패 (targetId=" + target.getTargetId() + "): " + result.resultMessage(), operatorId);
                return false;
            }
        }

        application.changeStatus(RefinanceStatus.COMPLETED, operatorId);
        refinanceApplicationRepository.save(application);
        recordHistory(application.getApplicationId(), RefinanceStatus.REPAYING, RefinanceStatus.COMPLETED,
                "대환완료", "대환이 완료되었습니다.", operatorId);
        return true;
    }

    private void markError(RefinanceApplication application, Long transactionId, FailedStep failedStep, String errorCode, String errorMessage) {
        refinanceErrorRepository.save(RefinanceError.builder()
                .applicationId(application.getApplicationId())
                .transactionId(transactionId)
                .failedStep(failedStep)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .status(ErrorProcessStatus.FAILED)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void recordHistory(Long applicationId, RefinanceStatus from, RefinanceStatus to, String actionType, String description, String operatorId) {
        refinanceHistoryRepository.save(RefinanceHistory.builder()
                .applicationId(applicationId)
                .actionType(actionType)
                .fromStatus(from)
                .toStatus(to)
                .description(description)
                .processedBy(operatorId)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
