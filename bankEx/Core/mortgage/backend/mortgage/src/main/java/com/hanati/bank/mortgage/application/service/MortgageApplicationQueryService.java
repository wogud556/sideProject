package com.hanati.bank.mortgage.application.service;

import com.hanati.bank.mortgage.application.dto.ApplicationDetailResponse;
import com.hanati.bank.mortgage.application.dto.ApplicationSummary;
import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.repository.MortgageApplicationRepository;
import com.hanati.bank.mortgage.approval.repository.MortgageAgreementRepository;
import com.hanati.bank.mortgage.approval.repository.MortgageApprovalRepository;
import com.hanati.bank.mortgage.collateral.repository.MortgageLienRepository;
import com.hanati.bank.mortgage.collateral.repository.MortgageValuationRepository;
import com.hanati.bank.mortgage.execution.entity.MortgageLoanAccount;
import com.hanati.bank.mortgage.execution.repository.MortgageExecutionRepository;
import com.hanati.bank.mortgage.execution.repository.MortgageLoanAccountRepository;
import com.hanati.bank.mortgage.limit.repository.MortgageLimitResultRepository;
import com.hanati.bank.mortgage.screening.repository.MortgageScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MortgageApplicationQueryService {

    private final MortgageApplicationLoader loader;
    private final MortgageApplicationRepository applicationRepository;
    private final MortgageValuationRepository valuationRepository;
    private final MortgageLienRepository lienRepository;
    private final MortgageLimitResultRepository limitResultRepository;
    private final MortgageScreeningRepository screeningRepository;
    private final MortgageApprovalRepository approvalRepository;
    private final MortgageAgreementRepository agreementRepository;
    private final MortgageExecutionRepository executionRepository;
    private final MortgageLoanAccountRepository loanAccountRepository;

    /** 담당자 목록. 상태를 지정하면 접수순(오래된 건 먼저), 생략하면 최신순 */
    public List<ApplicationSummary> getApplications(ApplicationStatus status) {
        List<MortgageApplication> rows = status == null
                ? applicationRepository.findAllByOrderByApplicationIdDesc()
                : applicationRepository.findByStatusOrderByApplicationIdAsc(status);
        return rows.stream().map(ApplicationSummary::from).toList();
    }

    public List<ApplicationSummary> getCustomerApplications(String customerId) {
        return applicationRepository.findByCustomerIdOrderByApplicationIdDesc(customerId).stream()
                .map(ApplicationSummary::from)
                .toList();
    }

    public ApplicationDetailResponse getDetail(Long applicationId) {
        MortgageApplication a = loader.get(applicationId);
        MortgageProperty property = loader.property(applicationId);
        Long propertyId = property.getPropertyId();
        return ApplicationDetailResponse.of(a, property, loader.income(applicationId), loader.debts(applicationId),
                valuationRepository.findByPropertyIdOrderByValuationIdDesc(propertyId),
                lienRepository.findByPropertyIdOrderByLienRankAsc(propertyId),
                limitResultRepository.findFirstByApplicationIdOrderByLimitResultIdDesc(applicationId).orElse(null),
                screeningRepository.findByApplicationIdOrderByScreeningIdDesc(applicationId),
                approvalRepository.findByApplicationId(applicationId).orElse(null),
                agreementRepository.findByApplicationId(applicationId).orElse(null),
                executionRepository.findByApplicationIdOrderByExecutionIdDesc(applicationId),
                loanAccountRepository.findByApplicationId(applicationId)
                        .map(MortgageLoanAccount::getLoanAccountId).orElse(null));
    }
}
