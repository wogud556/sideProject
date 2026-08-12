package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.service.OperatorAuthService;
import com.hanati.bank.refinance.refinance.domain.ErrorProcessStatus;
import com.hanati.bank.refinance.refinance.domain.FailedStep;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.entity.RefinanceError;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceErrorRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 실패한 대환 신청의 재처리 (명세 19번). 처음부터 전체 프로세스를 다시 실행하지 않고,
 * TB_REFINANCE_ERROR에 기록된 실패 단계부터만 재개한다. failedStep=EXISTING_LOAN_REPAYMENT인 경우
 * 신규대출 실행 단계(RefinanceExecutionService#executeNewLoanStep)는 절대 다시 호출하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RefinanceRetryService {

    private static final int MAX_RETRY_BEFORE_MANUAL_CHECK = 3;

    private final RefinanceApplicationRepository refinanceApplicationRepository;
    private final RefinanceErrorRepository refinanceErrorRepository;
    private final RefinanceHistoryRepository refinanceHistoryRepository;
    private final RefinanceApplicationService refinanceApplicationService;
    private final RefinanceExecutionService refinanceExecutionService;
    private final OperatorAuthService operatorAuthService;
    private final AuditLogService auditLogService;

    @Transactional
    public RefinanceApplicationResponse retry(Long applicationId, String operatorId) {
        operatorAuthService.requireRole(operatorId, OperatorRole.ROLE_OPERATOR);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        if (application.getStatus() != RefinanceStatus.FAILED) {
            throw new BusinessException(ErrorCode.NOT_RETRYABLE);
        }

        RefinanceError error = refinanceErrorRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_RECORD_NOT_FOUND));
        if (error.getStatus() == ErrorProcessStatus.MANUAL_CHECK_REQUIRED) {
            throw new BusinessException(ErrorCode.NOT_RETRYABLE);
        }

        error.setStatus(ErrorProcessStatus.RETRYING);
        error.setUpdatedAt(LocalDateTime.now());
        refinanceErrorRepository.save(error);

        boolean success;
        if (error.getFailedStep() == FailedStep.NEW_LOAN_EXECUTION) {
            success = retryFromNewLoanExecution(applicationId, operatorId);
        } else {
            success = retryFromExistingLoanRepayment(applicationId, operatorId);
        }

        finalizeRetryResult(applicationId, error, success, operatorId);

        return refinanceApplicationService.toResponse(refinanceApplicationService.getApplicationOrThrow(applicationId));
    }

    private boolean retryFromNewLoanExecution(Long applicationId, String operatorId) {
        int updated = refinanceApplicationRepository.updateStatusIfMatch(applicationId, RefinanceStatus.FAILED, RefinanceStatus.EXECUTING);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        recordHistory(applicationId, RefinanceStatus.FAILED, RefinanceStatus.EXECUTING, "재처리", "신규대출 실행 재처리를 시작합니다.", operatorId);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        boolean newLoanOk = refinanceExecutionService.executeNewLoanStep(application, operatorId);
        if (!newLoanOk) {
            return false;
        }

        application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        refinanceExecutionService.transitionToRepaying(application, operatorId);
        application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        return refinanceExecutionService.repayExistingLoansStep(application, operatorId);
    }

    private boolean retryFromExistingLoanRepayment(Long applicationId, String operatorId) {
        int updated = refinanceApplicationRepository.updateStatusIfMatch(applicationId, RefinanceStatus.FAILED, RefinanceStatus.REPAYING);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        recordHistory(applicationId, RefinanceStatus.FAILED, RefinanceStatus.REPAYING, "재처리", "기존대출 상환 재처리를 시작합니다. (신규대출 재실행 없음)", operatorId);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        return refinanceExecutionService.repayExistingLoansStep(application, operatorId);
    }

    private void finalizeRetryResult(Long applicationId, RefinanceError originalError, boolean success, String operatorId) {
        Long customerId = refinanceApplicationService.getApplicationOrThrow(applicationId).getCustomerId();

        if (success) {
            originalError.setStatus(ErrorProcessStatus.SUCCESS);
            originalError.setUpdatedAt(LocalDateTime.now());
            refinanceErrorRepository.save(originalError);
            auditLogService.record(operatorId, "재처리", customerId, applicationId, "재처리 성공");
            return;
        }

        // repayExistingLoansStep/executeNewLoanStep 실패 시 내부에서 새 RefinanceError 행을 생성했으므로 최신 행을 가져와 재시도 횟수를 누적한다.
        RefinanceError latestError = refinanceErrorRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElse(originalError);
        int nextRetryCount = (originalError.getRetryCount() == null ? 0 : originalError.getRetryCount()) + 1;
        latestError.setRetryCount(nextRetryCount);
        latestError.setStatus(nextRetryCount >= MAX_RETRY_BEFORE_MANUAL_CHECK
                ? ErrorProcessStatus.MANUAL_CHECK_REQUIRED
                : ErrorProcessStatus.FAILED);
        latestError.setUpdatedAt(LocalDateTime.now());
        refinanceErrorRepository.save(latestError);

        auditLogService.record(operatorId, "재처리", customerId, applicationId,
                "재처리 실패 (" + nextRetryCount + "회차, " + latestError.getStatus() + ")");
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
