package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.JeonseApplicationNoGenerator;
import com.hanati.bank.bankEx.common.util.JeonseContractNoGenerator;
import com.hanati.bank.bankEx.dto.JeonseContractRequest;
import com.hanati.bank.bankEx.dto.JeonseLoanApplyRequest;
import com.hanati.bank.bankEx.dto.JeonseLoanApplyResponse;
import com.hanati.bank.bankEx.dto.JeonseLoanExecuteResponse;
import com.hanati.bank.bankEx.dto.JeonseLoanProductResponse;
import com.hanati.bank.bankEx.dto.JeonseLoanReviewResponse;
import com.hanati.bank.bankEx.entity.JeonseContract;
import com.hanati.bank.bankEx.entity.JeonseLoanApplication;
import com.hanati.bank.bankEx.entity.JeonseLoanProduct;
import com.hanati.bank.bankEx.entity.JeonseRepaymentSchedule;
import com.hanati.bank.bankEx.enums.GuaranteeOrg;
import com.hanati.bank.bankEx.enums.JeonseLoanStatus;
import com.hanati.bank.bankEx.enums.RejectReasonCode;
import com.hanati.bank.bankEx.enums.RepaymentType;
import com.hanati.bank.bankEx.repository.JeonseContractRepository;
import com.hanati.bank.bankEx.repository.JeonseLoanApplicationRepository;
import com.hanati.bank.bankEx.repository.JeonseLoanProductRepository;
import com.hanati.bank.bankEx.repository.JeonseRepaymentScheduleRepository;
import com.hanati.bank.bankEx.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class jeonseLoanService {

    private static final long CAPITAL_AREA_DEPOSIT_CEILING = 700_000_000L;
    private static final long NON_CAPITAL_AREA_DEPOSIT_CEILING = 500_000_000L;
    private static final double DOWN_PAYMENT_MIN_RATIO = 0.05;

    private final JeonseLoanProductRepository jeonseLoanProductRepository;
    private final JeonseLoanApplicationRepository jeonseLoanApplicationRepository;
    private final JeonseContractRepository jeonseContractRepository;
    private final JeonseRepaymentScheduleRepository jeonseRepaymentScheduleRepository;
    private final UserInfoRepository userInfoRepository;
    private final jeonseLimitCalculator jeonseLimitCalculator;
    private final jeonseRateCalculator jeonseRateCalculator;
    private final jeonseReviewService jeonseReviewService;
    private final jeonseRepaymentScheduleGenerator jeonseRepaymentScheduleGenerator;

    public List<JeonseLoanProductResponse> getProducts() {
        return jeonseLoanProductRepository.findAll().stream()
                .filter(p -> "Y".equals(p.getUseYn()))
                .map(p -> new JeonseLoanProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getProductType(),
                        p.getMaxLimitAmount(),
                        p.getBaseRate(),
                        p.getMinRate(),
                        p.getMaxRate()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public JeonseLoanApplyResponse apply(JeonseLoanApplyRequest request) {
        validateApplyRequest(request);
        JeonseContractRequest cr = request.getContract();

        JeonseLoanProduct product = jeonseLoanProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JEONSE_PRODUCT_NOT_FOUND));

        GuaranteeOrg guaranteeOrg = parseGuaranteeOrg(request.getGuaranteeOrg());
        RepaymentType repaymentType = request.getRepaymentType() == null
                ? RepaymentType.MATURITY
                : RepaymentType.valueOf(request.getRepaymentType());

        LocalDateTime now = LocalDateTime.now();
        String applicationId = JeonseApplicationNoGenerator.generate(jeonseLoanApplicationRepository::existsById);

        JeonseLoanApplication application = JeonseLoanApplication.builder()
                .applicationId(applicationId)
                .userId(request.getUserId())
                .productId(product.getProductId())
                .requestAmount(request.getRequestAmount())
                .annualIncome(request.getAnnualIncome())
                .existingDebtAmount(request.getExistingDebtAmount() == null ? 0L : request.getExistingDebtAmount())
                .creditScore(request.getCreditScore())
                .homelessYn(request.getHomelessYn())
                .householderYn(request.getHouseholderYn())
                .guaranteeOrg(guaranteeOrg)
                .repaymentType(repaymentType)
                .status(JeonseLoanStatus.VALIDATING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        String contractId = JeonseContractNoGenerator.generate(jeonseContractRepository::existsById);
        JeonseContract contract = JeonseContract.builder()
                .contractId(contractId)
                .applicationId(applicationId)
                .lessorName(cr.getLessorName())
                .lessorPhone(cr.getLessorPhone())
                .houseAddress(cr.getHouseAddress())
                .houseType(cr.getHouseType())
                .capitalAreaYn(cr.getCapitalAreaYn())
                .depositAmount(cr.getDepositAmount())
                .downPaymentAmount(cr.getDownPaymentAmount())
                .contractStartDate(cr.getContractStartDate())
                .contractEndDate(cr.getContractEndDate())
                .fixedDateYn(cr.getFixedDateYn())
                .moveInPlanYn(cr.getMoveInPlanYn())
                .seniorClaimAmount(cr.getSeniorClaimAmount() == null ? 0L : cr.getSeniorClaimAmount())
                .createdAt(now)
                .build();

        RejectReasonCode eligibilityReject = checkEligibility(request);
        if (eligibilityReject != null) {
            return rejectApplication(application, contract, eligibilityReject);
        }

        RejectReasonCode contractReject = checkContract(cr);
        if (contractReject != null) {
            return rejectApplication(application, contract, contractReject);
        }

        if (!"Y".equals(cr.getFixedDateYn())) {
            application.setRejectReasonCode(RejectReasonCode.NO_FIXED_DATE);
            application.setUpdatedAt(LocalDateTime.now());
            jeonseLoanApplicationRepository.save(application);
            jeonseContractRepository.save(contract);
            return new JeonseLoanApplyResponse(applicationId, application.getStatus().name(), null,
                    request.getRequestAmount(), null, RejectReasonCode.NO_FIXED_DATE.getMessage());
        }

        double debtRatio = debtRatio(application.getExistingDebtAmount(), request.getAnnualIncome());
        if (request.getCreditScore() < 600) {
            return rejectApplication(application, contract, RejectReasonCode.LOW_CREDIT_SCORE);
        }
        if (debtRatio >= 0.70) {
            return rejectApplication(application, contract, RejectReasonCode.HIGH_DEBT_RATIO);
        }

        long limit = jeonseLimitCalculator.calculate(cr.getDepositAmount(), request.getAnnualIncome(),
                application.getExistingDebtAmount(), product.getMaxLimitAmount());
        application.setAvailableLimitAmount(limit);
        if (limit <= 0 || request.getRequestAmount() > limit) {
            return rejectApplication(application, contract, RejectReasonCode.LIMIT_SHORTAGE);
        }

        double rate = jeonseRateCalculator.calculate(request.getCreditScore(), debtRatio, guaranteeOrg,
                request.isSalaryTransferYn(), request.isCardUsageYn(), request.isAutoTransferYn());

        application.setStatus(JeonseLoanStatus.LIMIT_CALCULATED);
        application.setLoanRate(rate);
        application.setUpdatedAt(LocalDateTime.now());
        jeonseLoanApplicationRepository.save(application);
        jeonseContractRepository.save(contract);

        return new JeonseLoanApplyResponse(applicationId, application.getStatus().name(), limit,
                request.getRequestAmount(), rate, "전세대출 신청 및 한도 산출이 완료되었습니다.");
    }

    @Transactional
    public JeonseLoanReviewResponse review(String applicationId) {
        JeonseLoanApplication application = getApplication(applicationId);
        if (application.getStatus() != JeonseLoanStatus.LIMIT_CALCULATED) {
            throw new BusinessException(ErrorCode.JEONSE_INVALID_APPLICATION_STATE);
        }
        JeonseContract contract = jeonseContractRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JEONSE_APPLICATION_NOT_FOUND));

        long approvedAmount = Math.min(application.getRequestAmount(), application.getAvailableLimitAmount());
        application.setApprovedAmount(approvedAmount);
        application.setStatus(JeonseLoanStatus.GUARANTEE_REVIEW);

        double debtRatio = debtRatio(application.getExistingDebtAmount(), application.getAnnualIncome());
        Optional<RejectReasonCode> guaranteeReject = jeonseReviewService.reviewGuarantee(application, contract, debtRatio);
        if (guaranteeReject.isPresent()) {
            return rejectReview(application, guaranteeReject.get());
        }

        application.setStatus(JeonseLoanStatus.BANK_REVIEW);
        Optional<RejectReasonCode> bankReject = jeonseReviewService.reviewBank(application);
        if (bankReject.isPresent()) {
            return rejectReview(application, bankReject.get());
        }

        application.setStatus(JeonseLoanStatus.APPROVED);
        application.setUpdatedAt(LocalDateTime.now());
        jeonseLoanApplicationRepository.save(application);

        return new JeonseLoanReviewResponse(application.getApplicationId(), application.getStatus().name(),
                application.getApprovedAmount(), application.getLoanRate(), "전세대출 심사가 승인되었습니다.");
    }

    @Transactional
    public JeonseLoanExecuteResponse execute(String applicationId) {
        JeonseLoanApplication application = getApplication(applicationId);
        if (application.getStatus() != JeonseLoanStatus.APPROVED) {
            throw new BusinessException(ErrorCode.JEONSE_INVALID_APPLICATION_STATE);
        }
        JeonseContract contract = jeonseContractRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JEONSE_APPLICATION_NOT_FOUND));

        application.setStatus(JeonseLoanStatus.EXECUTED);
        application.setUpdatedAt(LocalDateTime.now());
        jeonseLoanApplicationRepository.save(application);

        LocalDate firstPaymentDate = jeonseRepaymentScheduleGenerator.firstPaymentDate(LocalDate.now());
        List<JeonseRepaymentSchedule> schedules = jeonseRepaymentScheduleGenerator.generate(application, contract, firstPaymentDate);
        jeonseRepaymentScheduleRepository.saveAll(schedules);

        return new JeonseLoanExecuteResponse(application.getApplicationId(), application.getStatus().name(),
                application.getApprovedAmount(), firstPaymentDate, "전세대출 실행이 완료되었습니다.");
    }

    private JeonseLoanApplyResponse rejectApplication(JeonseLoanApplication application, JeonseContract contract, RejectReasonCode reason) {
        application.setStatus(JeonseLoanStatus.REJECTED);
        application.setRejectReasonCode(reason);
        application.setUpdatedAt(LocalDateTime.now());
        jeonseLoanApplicationRepository.save(application);
        jeonseContractRepository.save(contract);
        return new JeonseLoanApplyResponse(application.getApplicationId(), application.getStatus().name(), 0L,
                application.getRequestAmount(), null, reason.getMessage());
    }

    private JeonseLoanReviewResponse rejectReview(JeonseLoanApplication application, RejectReasonCode reason) {
        application.setStatus(JeonseLoanStatus.REJECTED);
        application.setRejectReasonCode(reason);
        application.setUpdatedAt(LocalDateTime.now());
        jeonseLoanApplicationRepository.save(application);
        return new JeonseLoanReviewResponse(application.getApplicationId(), application.getStatus().name(),
                null, application.getLoanRate(), reason.getMessage());
    }

    private RejectReasonCode checkEligibility(JeonseLoanApplyRequest request) {
        if (!userInfoRepository.existsById(request.getUserId())) {
            return RejectReasonCode.NOT_LOGIN;
        }
        if (!"Y".equals(request.getHouseholderYn())) {
            return RejectReasonCode.NOT_HOUSEHOLDER;
        }
        if (!"Y".equals(request.getHomelessYn())) {
            return RejectReasonCode.NOT_HOMELESS;
        }
        return null;
    }

    private RejectReasonCode checkContract(JeonseContractRequest cr) {
        if (!cr.getContractEndDate().isAfter(cr.getContractStartDate())) {
            return RejectReasonCode.INVALID_CONTRACT_DATE;
        }
        long minDownPayment = Math.round(cr.getDepositAmount() * DOWN_PAYMENT_MIN_RATIO);
        if (cr.getDownPaymentAmount() < minDownPayment) {
            return RejectReasonCode.DOWN_PAYMENT_SHORTAGE;
        }
        boolean capitalArea = "Y".equals(cr.getCapitalAreaYn());
        long depositCeiling = capitalArea ? CAPITAL_AREA_DEPOSIT_CEILING : NON_CAPITAL_AREA_DEPOSIT_CEILING;
        if (cr.getDepositAmount() > depositCeiling) {
            return RejectReasonCode.DEPOSIT_LIMIT_EXCEEDED;
        }
        return null;
    }

    private double debtRatio(long existingDebtAmount, long annualIncome) {
        if (annualIncome <= 0) {
            return 1.0;
        }
        return (double) existingDebtAmount / annualIncome;
    }

    private GuaranteeOrg parseGuaranteeOrg(String guaranteeOrg) {
        try {
            return GuaranteeOrg.valueOf(guaranteeOrg);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private JeonseLoanApplication getApplication(String applicationId) {
        return jeonseLoanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JEONSE_APPLICATION_NOT_FOUND));
    }

    private void validateApplyRequest(JeonseLoanApplyRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getRequestAmount() == null || request.getRequestAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (request.getAnnualIncome() == null || request.getAnnualIncome() < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (request.getCreditScore() == null || request.getCreditScore() < 1 || request.getCreditScore() > 1000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        JeonseContractRequest cr = request.getContract();
        if (cr == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (cr.getDepositAmount() == null || cr.getDepositAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (cr.getDownPaymentAmount() == null || cr.getDownPaymentAmount() < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (cr.getContractStartDate() == null || cr.getContractEndDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
