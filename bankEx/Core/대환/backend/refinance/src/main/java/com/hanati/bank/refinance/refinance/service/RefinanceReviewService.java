package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.service.OperatorAuthService;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.EligibilityResponse;
import com.hanati.bank.refinance.refinance.dto.RefinanceApplicationResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import com.hanati.bank.refinance.refinance.entity.RefinanceReview;
import com.hanati.bank.refinance.refinance.entity.RefinanceTarget;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceReviewRepository;
import com.hanati.bank.refinance.refinance.repository.RefinanceTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceReviewService {

    private final RefinanceApplicationRepository refinanceApplicationRepository;
    private final RefinanceTargetRepository refinanceTargetRepository;
    private final RefinanceReviewRepository refinanceReviewRepository;
    private final RefinanceHistoryRepository refinanceHistoryRepository;
    private final RefinanceApplicationService refinanceApplicationService;
    private final RefinanceEligibilityService refinanceEligibilityService;
    private final OperatorAuthService operatorAuthService;
    private final AuditLogService auditLogService;

    @Transactional
    public RefinanceApplicationResponse review(Long applicationId, String opinion, String operatorId) {
        operatorAuthService.requireRole(operatorId, OperatorRole.ROLE_REVIEWER);

        RefinanceApplication application = refinanceApplicationService.getApplicationOrThrow(applicationId);
        application.changeStatus(RefinanceStatus.REVIEWING, operatorId);

        List<Long> loanIds = refinanceTargetRepository.findByApplicationId(applicationId).stream()
                .map(RefinanceTarget::getLoanId).toList();
        EligibilityResponse eligibility = refinanceEligibilityService.check(application.getCustomerId(), loanIds, applicationId);

        try {
            refinanceApplicationRepository.save(application);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }

        refinanceReviewRepository.save(RefinanceReview.builder()
                .applicationId(applicationId)
                .reviewerId(operatorId)
                .reviewedAt(LocalDateTime.now())
                .eligibilitySnapshot(formatSnapshot(eligibility))
                .opinion(opinion)
                .build());

        refinanceHistoryRepository.save(RefinanceHistory.builder()
                .applicationId(applicationId)
                .actionType("심사")
                .fromStatus(RefinanceStatus.REQUESTED)
                .toStatus(RefinanceStatus.REVIEWING)
                .description("심사 처리 (" + (eligibility.eligible() ? "적격" : "부적격") + ")")
                .processedBy(operatorId)
                .processedAt(LocalDateTime.now())
                .build());

        auditLogService.record(operatorId, "심사", application.getCustomerId(), applicationId, "대환 신청 심사 처리");

        return refinanceApplicationService.toResponse(application);
    }

    private String formatSnapshot(EligibilityResponse eligibility) {
        StringBuilder sb = new StringBuilder("eligible=").append(eligibility.eligible());
        eligibility.results().forEach(r -> sb.append(" | ")
                .append(r.code()).append('=').append(r.passed()));
        return sb.toString();
    }
}
