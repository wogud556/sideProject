package com.hanati.bank.mortgage.collateral.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.collateral.dto.CollateralEvaluationRequest;
import com.hanati.bank.mortgage.collateral.entity.MortgageLien;
import com.hanati.bank.mortgage.collateral.entity.MortgageValuation;
import com.hanati.bank.mortgage.collateral.enums.LienType;
import com.hanati.bank.mortgage.collateral.repository.MortgageLienRepository;
import com.hanati.bank.mortgage.collateral.repository.MortgageValuationRepository;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollateralService {

    private static final List<LienType> SENIOR_TYPES = List.of(LienType.SENIOR_MORTGAGE, LienType.LEASE_DEPOSIT);

    private final MortgageApplicationLoader loader;
    private final MortgageValuationRepository valuationRepository;
    private final MortgageLienRepository lienRepository;
    private final OperatorGuard operatorGuard;

    /** 담보 평가 등록. 재평가하면 상태가 COLLATERAL_EVALUATED로 돌아가 한도를 다시 산정해야 한다. */
    @Transactional
    public void evaluate(Long applicationId, String operatorId, CollateralEvaluationRequest req, LocalDate today) {
        operatorGuard.require(operatorId, OperatorRole.OPERATIONS);
        MortgageApplication application = loader.lock(applicationId);
        application.requireTransitionTo(ApplicationStatus.COLLATERAL_EVALUATED);
        MortgageProperty property = loader.property(applicationId);
        LocalDateTime now = LocalDateTime.now();

        valuationRepository.save(MortgageValuation.builder()
                .propertyId(property.getPropertyId())
                .valuationMethod(req.valuationMethod())
                .appraisedValue(req.appraisedValue())
                .valuationDate(today)
                .valuedBy(operatorId)
                .createdAt(now)
                .build());

        lienRepository.deleteByPropertyIdAndLienTypeIn(property.getPropertyId(), SENIOR_TYPES);
        req.seniorClaims().forEach(c -> lienRepository.save(MortgageLien.builder()
                .propertyId(property.getPropertyId())
                .lienType(c.lienType())
                .creditor(c.creditor())
                .amount(c.amount())
                .lienRank(c.lienRank())
                .registeredBy(operatorId)
                .registeredAt(now)
                .build()));

        application.changeStatus(ApplicationStatus.COLLATERAL_EVALUATED, now);
    }

    public MortgageValuation latestValuation(Long propertyId) {
        return valuationRepository.findFirstByPropertyIdOrderByValuationIdDesc(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALUATION_NOT_FOUND));
    }

    /** 담보가용액에서 차감할 선순위 채권·임차보증금 합계 */
    public long seniorClaimTotal(Long propertyId) {
        return lienRepository.findByPropertyIdOrderByLienRankAsc(propertyId).stream()
                .filter(l -> l.getLienType().isSeniorClaim())
                .mapToLong(MortgageLien::getAmount)
                .sum();
    }

    /** 당행 근저당 설정 등록. 순위는 기존 권리 다음이다 */
    public MortgageLien registerOwnMortgage(Long propertyId, long maxClaimAmount, String operatorId, LocalDateTime now) {
        int nextRank = lienRepository.findByPropertyIdOrderByLienRankAsc(propertyId).stream()
                .mapToInt(MortgageLien::getLienRank).max().orElse(0) + 1;
        return lienRepository.save(MortgageLien.builder()
                .propertyId(propertyId)
                .lienType(LienType.OWN_MORTGAGE)
                .creditor("하나은행")
                .amount(maxClaimAmount)
                .lienRank(nextRank)
                .registeredBy(operatorId)
                .registeredAt(now)
                .build());
    }
}
