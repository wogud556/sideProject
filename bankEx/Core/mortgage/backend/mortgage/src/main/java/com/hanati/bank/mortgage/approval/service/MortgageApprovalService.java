package com.hanati.bank.mortgage.approval.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.approval.dto.ApprovalRequest;
import com.hanati.bank.mortgage.approval.entity.MortgageApproval;
import com.hanati.bank.mortgage.approval.repository.MortgageApprovalRepository;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.limit.entity.MortgageLimitResult;
import com.hanati.bank.mortgage.limit.repository.MortgageLimitResultRepository;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.product.MortgageProduct;
import com.hanati.bank.mortgage.screening.entity.MortgageScreening;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.service.MortgageScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MortgageApprovalService {

    /** 승인 유효기간 (승인일 포함 30일) */
    static final int APPROVAL_VALID_DAYS = 30;

    private final MortgageApplicationLoader loader;
    private final MortgageScreeningService screeningService;
    private final MortgageLimitResultRepository limitResultRepository;
    private final MortgageApprovalRepository approvalRepository;
    private final OperatorGuard operatorGuard;

    /**
     * 대출 승인. 심사역(UNDERWRITER)과 승인권자(APPROVER)는 역할이 달라 같은 사람이 심사와 승인을 함께 할 수 없다.
     * 금리는 심사 근거가 된 한도 산정 결과의 적용 금리를 그대로 확정한다.
     */
    @Transactional
    public MortgageApproval approve(Long applicationId, String operatorId, ApprovalRequest req, LocalDate today) {
        operatorGuard.require(operatorId, OperatorRole.APPROVER);
        MortgageApplication application = loader.lock(applicationId);
        application.requireTransitionTo(ApplicationStatus.APPROVED);

        MortgageScreening screening = screeningService.latest(applicationId);
        MortgageProduct product = loader.product(application.getProductCode());
        long amount = req.approvedAmount() != null ? req.approvedAmount() : screening.getRecommendedAmount();
        if (amount < product.getMinAmount() || amount > screening.getRecommendedAmount()) {
            throw new BusinessException(ErrorCode.INVALID_APPROVED_AMOUNT);
        }
        MortgageLimitResult limit = limitResultRepository.findById(screening.getLimitResultId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LIMIT_RESULT_NOT_FOUND));

        List<ScreeningReason> reasons = MortgageScreeningService.reasons(screening);
        LocalDateTime now = LocalDateTime.now();
        MortgageApproval approval = approvalRepository.save(MortgageApproval.builder()
                .applicationId(applicationId)
                .screeningId(screening.getScreeningId())
                .approvedAmount(amount)
                .interestRate(limit.getAppliedRate())
                .termMonths(application.getTermMonths())
                .repaymentMethod(application.getRepaymentMethod())
                .conditions(reasons.isEmpty() ? null
                        : reasons.stream().map(ScreeningReason::getMessage).collect(Collectors.joining(" / ")))
                .conditionsFulfilled(!screening.getRequiresPreExecutionCheck())
                .approverId(operatorId)
                .approvalComment(req.comment())
                .approvedAt(now)
                .expiresOn(today.plusDays(APPROVAL_VALID_DAYS - 1))
                .build());

        application.changeStatus(ApplicationStatus.APPROVED, now);
        return approval;
    }

    public MortgageApproval get(Long applicationId) {
        return approvalRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));
    }
}
