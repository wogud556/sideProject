package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.service.OperatorAuthService;
import com.hanati.bank.refinance.refinance.domain.ApprovalDecision;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.ApprovalRequest;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.dto.RejectRequest;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.entity.RefinanceApproval;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceApprovalRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefinanceApprovalService {

    private final RefinanceApplicationRepository refinanceApplicationRepository;
    private final RefinanceApprovalRepository refinanceApprovalRepository;
    private final RefinanceHistoryRepository refinanceHistoryRepository;
    private final RefinanceApplicationService refinanceApplicationService;
    private final OperatorAuthService operatorAuthService;
    private final AuditLogService auditLogService;

    @Transactional
    public RefinanceApplicationResponse approve(Long applicationId, ApprovalRequest request, String operatorId) {
        operatorAuthService.requireRole(operatorId, OperatorRole.ROLE_APPROVER);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        application.changeStatus(RefinanceStatus.APPROVED, operatorId);

        BigDecimal approvedAmount = request.getApprovedAmount() != null
                ? request.getApprovedAmount()
                : application.getRequestedAmount();
        application.setApprovedAmount(approvedAmount);

        saveWithLockCheck(application);

        refinanceApprovalRepository.save(RefinanceApproval.builder()
                .applicationId(applicationId)
                .decision(ApprovalDecision.APPROVE)
                .approverId(operatorId)
                .approvedAt(LocalDateTime.now())
                .approvedAmount(approvedAmount)
                .approvalCondition(request.getApprovalCondition())
                .approvalMemo(request.getApprovalMemo())
                .build());

        recordHistoryAndAudit(applicationId, application.getCustomerId(), RefinanceStatus.REVIEWING, RefinanceStatus.APPROVED,
                "승인", "대환 신청이 승인되었습니다. (승인금액 " + approvedAmount + "원)", operatorId);

        return refinanceApplicationService.toResponse(application);
    }

    @Transactional
    public RefinanceApplicationResponse reject(Long applicationId, RejectRequest request, String operatorId) {
        operatorAuthService.requireRole(operatorId, OperatorRole.ROLE_APPROVER);
        if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        application.changeStatus(RefinanceStatus.REJECTED, operatorId);
        application.setRejectReasonCode("APPROVER_REJECTED");

        saveWithLockCheck(application);

        refinanceApprovalRepository.save(RefinanceApproval.builder()
                .applicationId(applicationId)
                .decision(ApprovalDecision.REJECT)
                .approverId(operatorId)
                .approvedAt(LocalDateTime.now())
                .rejectReason(request.getRejectReason())
                .build());

        recordHistoryAndAudit(applicationId, application.getCustomerId(), RefinanceStatus.REVIEWING, RefinanceStatus.REJECTED,
                "거절", "대환 신청이 거절되었습니다. 사유: " + request.getRejectReason(), operatorId);

        return refinanceApplicationService.toResponse(application);
    }

    private void saveWithLockCheck(RefinanceApplication application) {
        try {
            refinanceApplicationRepository.save(application);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    private void recordHistoryAndAudit(Long applicationId, Long customerId, RefinanceStatus from, RefinanceStatus to,
                                        String actionType, String description, String operatorId) {
        refinanceHistoryRepository.save(RefinanceHistory.builder()
                .applicationId(applicationId)
                .actionType(actionType)
                .fromStatus(from)
                .toStatus(to)
                .description(description)
                .processedBy(operatorId)
                .processedAt(LocalDateTime.now())
                .build());
        auditLogService.record(operatorId, actionType, customerId, applicationId, description);
    }
}
