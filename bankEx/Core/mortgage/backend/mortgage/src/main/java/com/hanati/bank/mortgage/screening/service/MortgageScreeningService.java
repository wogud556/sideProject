package com.hanati.bank.mortgage.screening.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.limit.entity.MortgageLimitResult;
import com.hanati.bank.mortgage.limit.service.MortgageLimitService;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.screening.engine.MortgageScreeningEngine;
import com.hanati.bank.mortgage.screening.engine.ScreeningInput;
import com.hanati.bank.mortgage.screening.engine.ScreeningOutcome;
import com.hanati.bank.mortgage.screening.entity.MortgageScreening;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;
import com.hanati.bank.mortgage.screening.repository.MortgageScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MortgageScreeningService {

    private static final Map<ScreeningStatus, ApplicationStatus> NEXT_STATUS = Map.of(
            ScreeningStatus.APPROVED, ApplicationStatus.SCREENING_APPROVED,
            ScreeningStatus.CONDITIONALLY_APPROVED, ApplicationStatus.CONDITIONALLY_APPROVED,
            ScreeningStatus.ADDITIONAL_DOCUMENT_REQUIRED, ApplicationStatus.DOCUMENT_REQUIRED,
            ScreeningStatus.REJECTED, ApplicationStatus.REJECTED);

    private final MortgageApplicationLoader loader;
    private final MortgageLimitService limitService;
    private final MortgageScreeningEngine screeningEngine;
    private final MortgageScreeningRepository screeningRepository;
    private final OperatorGuard operatorGuard;

    @Transactional
    public MortgageScreening screen(Long applicationId, String operatorId, ScreeningRequest req) {
        operatorGuard.require(operatorId, OperatorRole.UNDERWRITER);
        MortgageApplication application = loader.lock(applicationId);
        // 한도 산정 전이면 전이 검사에서 걸러진다 (APPLIED·COLLATERAL_EVALUATED에서는 심사 상태로 갈 수 없다)
        application.requireTransitionTo(ApplicationStatus.REJECTED);
        MortgageLimitResult limit = limitService.latest(applicationId);

        ScreeningOutcome outcome = screeningEngine.screen(new ScreeningInput(
                limit.getCreditScore(), req.incomeVerified(), req.ownershipVerified(),
                limit.getCollateralLimit(), limit.getFinalLimit(), application.getRequestedAmount()));

        LocalDateTime now = LocalDateTime.now();
        MortgageScreening screening = screeningRepository.save(MortgageScreening.builder()
                .applicationId(applicationId)
                .limitResultId(limit.getLimitResultId())
                .status(outcome.status())
                .borrowerResult(outcome.borrowerResult())
                .collateralResult(outcome.collateralResult())
                .regulatoryResult(outcome.regulatoryResult())
                .reasonCodes(outcome.reasons().stream().map(Enum::name).collect(Collectors.joining(",")))
                .recommendedAmount(outcome.recommendedAmount())
                .requiresPreExecutionCheck(outcome.requiresPreExecutionCheck())
                .incomeVerified(req.incomeVerified())
                .ownershipVerified(req.ownershipVerified())
                .screenerId(operatorId)
                .screeningComment(req.comment())
                .screenedAt(now)
                .build());

        application.changeStatus(NEXT_STATUS.get(outcome.status()), now);
        return screening;
    }

    public List<MortgageScreening> history(Long applicationId) {
        return screeningRepository.findByApplicationIdOrderByScreeningIdDesc(applicationId);
    }

    public MortgageScreening latest(Long applicationId) {
        return screeningRepository.findFirstByApplicationIdOrderByScreeningIdDesc(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    public static List<ScreeningReason> reasons(MortgageScreening screening) {
        if (screening.getReasonCodes() == null || screening.getReasonCodes().isBlank()) {
            return List.of();
        }
        return Arrays.stream(screening.getReasonCodes().split(","))
                .map(ScreeningReason::valueOf)
                .toList();
    }
}
