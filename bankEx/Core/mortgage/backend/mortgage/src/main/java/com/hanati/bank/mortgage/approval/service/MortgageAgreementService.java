package com.hanati.bank.mortgage.approval.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.service.MortgageApplicationLoader;
import com.hanati.bank.mortgage.approval.dto.AgreementRequest;
import com.hanati.bank.mortgage.approval.dto.PreconditionRequest;
import com.hanati.bank.mortgage.approval.entity.MortgageAgreement;
import com.hanati.bank.mortgage.approval.entity.MortgageApproval;
import com.hanati.bank.mortgage.approval.repository.MortgageAgreementRepository;
import com.hanati.bank.mortgage.collateral.entity.MortgageLien;
import com.hanati.bank.mortgage.collateral.service.CollateralService;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.common.util.Money;
import com.hanati.bank.mortgage.common.util.MortgageNoGenerator;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MortgageAgreementService {

    /** 근저당 채권최고액 = 대출금 × 120% (은행 관행) */
    private static final BigDecimal MAX_CLAIM_PERCENT = BigDecimal.valueOf(120);

    private final MortgageApplicationLoader loader;
    private final MortgageApprovalService approvalService;
    private final CollateralService collateralService;
    private final MortgageAgreementRepository agreementRepository;
    private final OperatorGuard operatorGuard;

    /** 고객 약정. 승인 조건을 그대로 옮겨 적고 실행금 입금 계좌를 정한다. */
    @Transactional
    public MortgageAgreement agree(Long applicationId, AgreementRequest req, LocalDate today) {
        MortgageApplication application = loader.lockOwned(applicationId, req.customerId());
        application.requireTransitionTo(ApplicationStatus.AGREED);
        if (!Boolean.TRUE.equals(req.consent())) {
            throw new BusinessException(ErrorCode.AGREEMENT_CONSENT_REQUIRED);
        }
        MortgageApproval approval = approvalService.get(applicationId);
        if (approval.isExpiredOn(today)) {
            throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        MortgageAgreement agreement = agreementRepository.save(MortgageAgreement.builder()
                .applicationId(applicationId)
                .approvalId(approval.getApprovalId())
                .disbursementAccountNo(req.disbursementAccountNo())
                .agreedAmount(approval.getApprovedAmount())
                .interestRate(approval.getInterestRate())
                .termMonths(approval.getTermMonths())
                .repaymentMethod(approval.getRepaymentMethod())
                .agreedAt(now)
                .build());
        agreement.assignAgreementNo(MortgageNoGenerator.agreementNo(today, agreement.getAgreementId()));

        application.changeStatus(ApplicationStatus.AGREED, now);
        return agreement;
    }

    /**
     * 실행 선행조건 확인 (명세 7: 약정 → 선행조건 검증 → 담보권 설정 확인).
     * 당행 근저당 설정과 승인 조건 이행이 모두 확인되면 실행 대기로 넘어간다.
     * 조건 이행이 아직이면 근저당만 등록하고 약정 완료 상태에 머문다.
     */
    @Transactional
    public void confirmPreconditions(Long applicationId, String operatorId, PreconditionRequest req) {
        operatorGuard.require(operatorId, OperatorRole.OPERATIONS);
        MortgageApplication application = loader.lock(applicationId);
        application.requireTransitionTo(ApplicationStatus.EXECUTION_READY);
        MortgageAgreement agreement = get(applicationId);
        MortgageApproval approval = approvalService.get(applicationId);
        LocalDateTime now = LocalDateTime.now();

        if (!agreement.isLienRegistered()) {
            long maxClaim = Money.percentOfDown(agreement.getAgreedAmount(), MAX_CLAIM_PERCENT);
            MortgageLien lien = collateralService.registerOwnMortgage(
                    loader.property(applicationId).getPropertyId(), maxClaim, operatorId, now);
            agreement.confirmLien(lien.getLienId(), operatorId, now);
        }
        if (Boolean.TRUE.equals(req.conditionsFulfilled()) && !approval.getConditionsFulfilled()) {
            approval.fulfillConditions(operatorId);
        }
        if (approval.getConditionsFulfilled()) {
            application.changeStatus(ApplicationStatus.EXECUTION_READY, now);
        }
    }

    public MortgageAgreement get(Long applicationId) {
        return agreementRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGREEMENT_NOT_FOUND));
    }
}
