package com.hanati.bank.screening.service;

import com.hanati.bank.screening.dto.ReviewDecisionRequest;
import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.dto.ScreeningResponse;
import com.hanati.bank.screening.engine.LoanScreeningEngine;
import com.hanati.bank.screening.engine.ScreeningResult;
import com.hanati.bank.screening.entity.LoanScreening;
import com.hanati.bank.screening.enums.ScreeningStatus;
import com.hanati.bank.screening.exception.BusinessException;
import com.hanati.bank.screening.exception.ErrorCode;
import com.hanati.bank.screening.repository.LoanScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanScreeningService {

    private final LoanScreeningRepository screeningRepository;
    private final LoanScreeningEngine screeningEngine;

    @Transactional
    public ScreeningResponse screen(ScreeningRequest req) {
        ScreeningResult result = screeningEngine.screen(req);

        LoanScreening screening = LoanScreening.builder()
                .customerId(req.getCustomerId())
                .loanProductId(req.getLoanProductId())
                .loanType(req.getLoanType())
                .requestedAmount(req.getRequestedAmount())
                .annualIncome(req.getAnnualIncome())
                .creditScore(req.getCreditScore())
                .existingLoanAmount(req.getExistingLoanAmount())
                .status(result.getStatus())
                .approvedAmount(result.getApprovedAmount())
                .approvedInterestRate(result.getApprovedInterestRate())
                .reasonCode(result.getReasonCode())
                .screenedAt(LocalDateTime.now())
                .build();
        return new ScreeningResponse(screeningRepository.save(screening));
    }

    @Transactional(readOnly = true)
    public ScreeningResponse getScreening(Long screeningId) {
        return new ScreeningResponse(findScreening(screeningId));
    }

    /** status가 null이면 전체를 최신순으로, 지정하면 해당 상태를 접수순으로 돌려준다 */
    @Transactional(readOnly = true)
    public List<ScreeningResponse> getScreenings(ScreeningStatus status) {
        List<LoanScreening> screenings = status == null
                ? screeningRepository.findAllByOrderByScreeningIdDesc()
                : screeningRepository.findByStatusOrderByScreeningIdAsc(status);
        return screenings.stream().map(ScreeningResponse::new).toList();
    }

    @Transactional
    public ScreeningResponse approve(Long screeningId, ReviewDecisionRequest req) {
        LoanScreening screening = findManualReview(screeningId);

        long approvedAmount = req.getApprovedAmount() == null ? screening.getRequestedAmount() : req.getApprovedAmount();
        if (approvedAmount <= 0 || approvedAmount > screening.getRequestedAmount()) {
            throw new BusinessException(ErrorCode.INVALID_APPROVED_AMOUNT);
        }

        screening.approveByReviewer(req.getReviewerId(), approvedAmount,
                screeningEngine.interestRateFor(screening.getCreditScore()), req.getComment(), LocalDateTime.now());
        return new ScreeningResponse(screening);
    }

    @Transactional
    public ScreeningResponse reject(Long screeningId, ReviewDecisionRequest req) {
        LoanScreening screening = findManualReview(screeningId);
        screening.rejectByReviewer(req.getReviewerId(), req.getComment(), LocalDateTime.now());
        return new ScreeningResponse(screening);
    }

    private LoanScreening findScreening(Long screeningId) {
        return screeningRepository.findById(screeningId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    private LoanScreening findManualReview(Long screeningId) {
        LoanScreening screening = findScreening(screeningId);
        if (screening.getStatus() != ScreeningStatus.MANUAL_REVIEW) {
            throw new BusinessException(ErrorCode.SCREENING_NOT_MANUAL_REVIEW);
        }
        return screening;
    }
}
