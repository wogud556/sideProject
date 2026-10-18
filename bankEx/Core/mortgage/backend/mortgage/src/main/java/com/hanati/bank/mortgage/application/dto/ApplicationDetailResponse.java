package com.hanati.bank.mortgage.application.dto;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageDebt;
import com.hanati.bank.mortgage.application.entity.MortgageIncome;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.enums.BorrowerType;
import com.hanati.bank.mortgage.approval.entity.MortgageAgreement;
import com.hanati.bank.mortgage.approval.entity.MortgageApproval;
import com.hanati.bank.mortgage.collateral.entity.MortgageLien;
import com.hanati.bank.mortgage.collateral.entity.MortgageValuation;
import com.hanati.bank.mortgage.execution.entity.MortgageExecution;
import com.hanati.bank.mortgage.limit.entity.MortgageLimitResult;
import com.hanati.bank.mortgage.screening.entity.MortgageScreening;
import com.hanati.bank.mortgage.screening.enums.AreaResult;
import com.hanati.bank.mortgage.screening.enums.ScreeningReason;
import com.hanati.bank.mortgage.screening.enums.ScreeningStatus;
import com.hanati.bank.mortgage.screening.service.MortgageScreeningService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 신청 1건의 전 단계 정보. 담당자 화면과 고객 채널이 함께 쓴다.
 * 고객에게 내보낼 항목은 bankEx가 골라서 전달한다.
 * 아직 거치지 않은 단계는 null(단건) 또는 빈 목록이다.
 */
public record ApplicationDetailResponse(
        ApplicationSummary application,
        String repaymentMethod,
        Integer termMonths,
        String loanPurpose,
        Integer houseCount,
        Boolean firstHomeBuyer,
        BorrowerType borrowerType,
        MortgageProperty property,
        MortgageIncome income,
        List<MortgageDebt> debts,
        List<MortgageValuation> valuations,
        List<MortgageLien> liens,
        MortgageLimitResult limit,
        List<ScreeningView> screenings,
        MortgageApproval approval,
        MortgageAgreement agreement,
        List<MortgageExecution> executions,
        Long loanAccountId
) {
    public static ApplicationDetailResponse of(MortgageApplication a, MortgageProperty property, MortgageIncome income,
                                               List<MortgageDebt> debts, List<MortgageValuation> valuations,
                                               List<MortgageLien> liens, MortgageLimitResult limit,
                                               List<MortgageScreening> screenings, MortgageApproval approval,
                                               MortgageAgreement agreement, List<MortgageExecution> executions,
                                               Long loanAccountId) {
        return new ApplicationDetailResponse(ApplicationSummary.from(a), a.getRepaymentMethod().name(),
                a.getTermMonths(), a.getLoanPurpose().name(), a.getHouseCount(), a.getFirstHomeBuyer(),
                a.borrowerType(), property, income, debts, valuations, liens, limit,
                screenings.stream().map(ScreeningView::from).toList(), approval, agreement, executions, loanAccountId);
    }

    public record ScreeningView(
            Long screeningId,
            Long limitResultId,
            ScreeningStatus status,
            String statusLabel,
            AreaResult borrowerResult,
            AreaResult collateralResult,
            AreaResult regulatoryResult,
            List<Reason> reasons,
            Long recommendedAmount,
            Boolean requiresPreExecutionCheck,
            Boolean incomeVerified,
            Boolean ownershipVerified,
            String screenerId,
            String screeningComment,
            LocalDateTime screenedAt
    ) {
        public static ScreeningView from(MortgageScreening s) {
            return new ScreeningView(s.getScreeningId(), s.getLimitResultId(), s.getStatus(), s.getStatus().getLabel(),
                    s.getBorrowerResult(), s.getCollateralResult(), s.getRegulatoryResult(),
                    MortgageScreeningService.reasons(s).stream().map(Reason::from).toList(),
                    s.getRecommendedAmount(), s.getRequiresPreExecutionCheck(), s.getIncomeVerified(),
                    s.getOwnershipVerified(), s.getScreenerId(), s.getScreeningComment(), s.getScreenedAt());
        }
    }

    public record Reason(String code, String message) {
        static Reason from(ScreeningReason r) {
            return new Reason(r.name(), r.getMessage());
        }
    }
}
