package com.hanati.bank.mortgage.limit.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageDebt;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.enums.DebtType;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.collateral.service.CollateralService;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.limit.engine.LimitCalculation;
import com.hanati.bank.mortgage.limit.engine.LimitInput;
import com.hanati.bank.mortgage.limit.engine.MortgageLimitEngine;
import com.hanati.bank.mortgage.limit.engine.MortgageRateCalculator;
import com.hanati.bank.mortgage.limit.entity.MortgageLimitResult;
import com.hanati.bank.mortgage.limit.repository.MortgageLimitResultRepository;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.enums.PolicyType;
import com.hanati.bank.mortgage.policy.service.PolicyResolver;
import com.hanati.bank.mortgage.product.MortgageProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MortgageLimitService {

    private final MortgageApplicationLoader loader;
    private final CollateralService collateralService;
    private final PolicyResolver policyResolver;
    private final MortgageLimitEngine limitEngine;
    private final MortgageLimitResultRepository limitResultRepository;
    private final OperatorGuard operatorGuard;

    /**
     * 정책 엔진 순서 (명세 5): 기준일 → 소재지 규제지역 → 차주 유형 → 상품 → 정책 조회 → 한도 계산 → 정책 버전과 결과 저장
     */
    @Transactional
    public MortgageLimitResult calculate(Long applicationId, String operatorId, LimitCalculationRequest req, LocalDate baseDate) {
        operatorGuard.require(operatorId, OperatorRole.UNDERWRITER);
        MortgageApplication application = loader.lock(applicationId);
        application.requireTransitionTo(ApplicationStatus.LIMIT_CALCULATED);

        MortgageProduct product = loader.product(application.getProductCode());
        MortgageProperty property = loader.property(applicationId);
        long annualIncome = loader.income(applicationId).getAnnualIncome();
        List<MortgageDebt> debts = loader.debts(applicationId);
        long collateralValue = collateralService.latestValuation(property.getPropertyId()).getAppraisedValue();
        long seniorClaims = collateralService.seniorClaimTotal(property.getPropertyId());

        String region = property.getRegionCode().name();
        String borrower = application.borrowerType().name();
        String productCode = product.getProductCode();
        MortgagePolicy ltv = policyResolver.resolve(PolicyType.LTV, region, borrower, productCode, baseDate);
        MortgagePolicy dti = policyResolver.resolve(PolicyType.DTI, region, borrower, productCode, baseDate);
        MortgagePolicy dsr = policyResolver.resolve(PolicyType.DSR, region, borrower, productCode, baseDate);
        MortgagePolicy stress = policyResolver.resolve(PolicyType.STRESS_RATE, region, borrower, productCode, baseDate);
        MortgagePolicy maxAmount = policyResolver.resolve(PolicyType.MAX_LOAN_AMOUNT, region, borrower, productCode, baseDate);

        BigDecimal rate = MortgageRateCalculator.rateFor(product.getBaseRate(), req.creditScore());
        long existingDebtService = debts.stream().mapToLong(MortgageDebt::annualDebtService).sum();
        long existingDtiBurden = debts.stream()
                .mapToLong(d -> d.getDebtType() == DebtType.MORTGAGE ? d.annualDebtService() : d.getAnnualInterest())
                .sum();

        LimitCalculation calc = limitEngine.calculate(new LimitInput(
                collateralValue, seniorClaims, annualIncome, existingDebtService, existingDtiBurden,
                application.getRequestedAmount(), application.getTermMonths(), application.getRepaymentMethod(), rate,
                ltv.getPolicyValue(), dti.getPolicyValue(), dsr.getPolicyValue(), stress.getPolicyValue(),
                maxAmount.getPolicyValue().longValueExact(), product.getMaxAmount()));

        LocalDateTime now = LocalDateTime.now();
        MortgageLimitResult result = limitResultRepository.save(MortgageLimitResult.builder()
                .applicationId(applicationId)
                .baseDate(baseDate)
                .regionCode(property.getRegionCode())
                .borrowerType(application.borrowerType())
                .creditScore(req.creditScore())
                .appliedRate(rate)
                .collateralValue(collateralValue)
                .seniorClaims(seniorClaims)
                .annualIncome(annualIncome)
                .existingDebtService(existingDebtService)
                .requestedAmount(application.getRequestedAmount())
                .ltvPercent(ltv.getPolicyValue())
                .dtiPercent(dti.getPolicyValue())
                .dsrPercent(dsr.getPolicyValue())
                .stressRate(stress.getPolicyValue())
                .ltvPolicyId(ltv.getPolicyId())
                .dtiPolicyId(dti.getPolicyId())
                .dsrPolicyId(dsr.getPolicyId())
                .stressPolicyId(stress.getPolicyId())
                .maxAmountPolicyId(maxAmount.getPolicyId())
                .ltvLimit(calc.ltvLimit())
                .collateralLimit(calc.collateralLimit())
                .dtiLimit(calc.dtiLimit())
                .dsrLimit(calc.dsrLimit())
                .stressDsrLimit(calc.stressDsrLimit())
                .productLimit(calc.productLimit())
                .regulatoryLimit(calc.regulatoryLimit())
                .finalLimit(calc.finalLimit())
                .bindingConstraint(calc.bindingConstraint())
                .requestedLtv(calc.requestedLtv())
                .requestedDti(calc.requestedDti())
                .requestedDsr(calc.requestedDsr())
                .requestedStressDsr(calc.requestedStressDsr())
                .calculatedBy(operatorId)
                .calculatedAt(now)
                .build());

        application.changeStatus(ApplicationStatus.LIMIT_CALCULATED, now);
        return result;
    }

    public MortgageLimitResult latest(Long applicationId) {
        return limitResultRepository.findFirstByApplicationIdOrderByLimitResultIdDesc(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIMIT_RESULT_NOT_FOUND));
    }
}
