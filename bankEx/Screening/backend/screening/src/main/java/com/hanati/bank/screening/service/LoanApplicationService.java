package com.hanati.bank.screening.service;

import com.hanati.bank.screening.dto.LoanApplicationRequest;
import com.hanati.bank.screening.dto.LoanApplicationResponse;
import com.hanati.bank.screening.dto.ScreeningResponse;
import com.hanati.bank.screening.engine.LoanScreeningEngine;
import com.hanati.bank.screening.engine.ScreeningResult;
import com.hanati.bank.screening.entity.CustomerCreditInfo;
import com.hanati.bank.screening.entity.LoanApplication;
import com.hanati.bank.screening.entity.LoanScreeningResult;
import com.hanati.bank.screening.repository.CustomerCreditInfoRepository;
import com.hanati.bank.screening.repository.LoanApplicationRepository;
import com.hanati.bank.screening.repository.LoanScreeningResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanScreeningResultRepository screeningResultRepository;
    private final CustomerCreditInfoRepository creditInfoRepository;
    private final LoanScreeningEngine screeningEngine;

    @Transactional
    public LoanApplicationResponse apply(LoanApplicationRequest req) {
        LoanApplication application = LoanApplication.builder()
                .userId(req.getUserId())
                .productId(req.getProductId())
                .requestAmount(req.getRequestAmount())
                .applicationStatus("APPLIED")
                .build();
        return new LoanApplicationResponse(applicationRepository.save(application));
    }

    @Transactional
    public ScreeningResponse screen(Long applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 정보를 찾을 수 없습니다."));

        CustomerCreditInfo creditInfo = creditInfoRepository.findByUserId(application.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("신용 정보가 없습니다. 마이페이지에서 등록해 주세요."));

        application.setApplicationStatus("SCREENING");
        applicationRepository.save(application);

        ScreeningResult result = screeningEngine.screen(creditInfo, application);

        // 금리 산정: CSS점수에 따라 3.5~7.0% 사이
        BigDecimal interestRate = calculateInterestRate(result.getCssScore(), result.getResultStatus());

        LoanScreeningResult screeningResult = LoanScreeningResult.builder()
                .applicationId(applicationId)
                .cssScore(result.getCssScore())
                .dsrRate(BigDecimal.valueOf(result.getDsrRate()).setScale(2, RoundingMode.HALF_UP))
                .approvedAmount(result.getApprovedAmount())
                .interestRate(interestRate)
                .resultStatus(result.getResultStatus())
                .rejectReason(result.getRejectReason())
                .build();

        screeningResultRepository.save(screeningResult);

        application.setApplicationStatus(result.getResultStatus());
        applicationRepository.save(application);

        return new ScreeningResponse(screeningResult);
    }

    public List<LoanApplicationResponse> getMyApplications(String userId) {
        return applicationRepository.findByUserId(userId).stream()
                .map(LoanApplicationResponse::new)
                .toList();
    }

    public ScreeningResponse getScreeningResult(Long applicationId) {
        LoanScreeningResult result = screeningResultRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("심사 결과가 없습니다."));
        return new ScreeningResponse(result);
    }

    private BigDecimal calculateInterestRate(int cssScore, String status) {
        if ("REJECTED".equals(status)) return BigDecimal.ZERO;
        if (cssScore >= 90) return new BigDecimal("3.50");
        if (cssScore >= 80) return new BigDecimal("4.20");
        if (cssScore >= 70) return new BigDecimal("5.50");
        return new BigDecimal("7.00");
    }
}
