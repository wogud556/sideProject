package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.EligibilityResponse;
import com.hanati.bank.refinance.refinance.dto.EligibilityResult;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 대환 가능 여부 검증 (명세 8번). 결과는 Boolean 하나가 아니라 항목별 판정 리스트로 반환한다.
 */
@Service
@RequiredArgsConstructor
public class RefinanceEligibilityService {

    private static final EnumSet<RefinanceStatus> IN_PROGRESS_STATUSES = EnumSet.of(
            RefinanceStatus.DRAFT, RefinanceStatus.REQUESTED, RefinanceStatus.REVIEWING,
            RefinanceStatus.APPROVED, RefinanceStatus.EXECUTING, RefinanceStatus.NEW_LOAN_EXECUTED,
            RefinanceStatus.REPAYING, RefinanceStatus.FAILED
    );

    private final CustomerRepository customerRepository;
    private final ExistingLoanRepository existingLoanRepository;
    private final RefinanceApplicationRepository refinanceApplicationRepository;

    public EligibilityResponse check(Long customerId, List<Long> loanIds) {
        return check(customerId, loanIds, null);
    }

    /**
     * @param excludeApplicationId 심사(review) 단계에서 재검증할 때, 현재 심사 중인 신청 자신을
     *                              "이미 진행 중인 대환 신청"으로 오판하지 않도록 제외할 applicationId.
     */
    public EligibilityResponse check(Long customerId, List<Long> loanIds, Long excludeApplicationId) {
        List<EligibilityResult> results = new ArrayList<>();

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            results.add(new EligibilityResult(null, "CUSTOMER_NOT_FOUND", false, "고객을 찾을 수 없습니다."));
            return new EligibilityResponse(false, results);
        }
        boolean customerActive = "ACTIVE".equals(customer.getStatus());
        results.add(new EligibilityResult(null, "CUSTOMER_STATUS",
                customerActive, customerActive ? "정상 상태의 고객입니다." : "정상 상태의 고객이 아닙니다."));

        boolean noInProgressApplication = refinanceApplicationRepository
                .findByCustomerIdAndStatusIn(customerId, IN_PROGRESS_STATUSES).stream()
                .filter(app -> !app.getApplicationId().equals(excludeApplicationId))
                .findAny()
                .isEmpty();
        results.add(new EligibilityResult(null, "ALREADY_IN_PROGRESS",
                noInProgressApplication, noInProgressApplication
                        ? "진행 중인 대환 신청이 없습니다." : "이미 진행 중인 대환 신청이 존재합니다."));

        for (Long loanId : loanIds) {
            results.addAll(checkLoan(loanId, customerId));
        }

        boolean eligible = results.stream().allMatch(EligibilityResult::passed);
        return new EligibilityResponse(eligible, results);
    }

    private List<EligibilityResult> checkLoan(Long loanId, Long customerId) {
        List<EligibilityResult> results = new ArrayList<>();
        ExistingLoan loan = existingLoanRepository.findById(loanId).orElse(null);

        if (loan == null) {
            results.add(new EligibilityResult(loanId, "LOAN_NOT_FOUND", false, "대출을 찾을 수 없습니다."));
            return results;
        }
        if (!loan.getCustomerId().equals(customerId)) {
            results.add(new EligibilityResult(loanId, "LOAN_NOT_OWNED", false, "해당 고객의 대출이 아닙니다."));
            return results;
        }

        boolean statusActive = "ACTIVE".equals(loan.getStatus());
        results.add(new EligibilityResult(loanId, "LOAN_STATUS", statusActive,
                statusActive ? "정상 상태의 대출입니다." : "정상 상태의 대출이 아닙니다."));

        boolean hasBalance = loan.getCurrentBalance() != null && loan.getCurrentBalance().signum() > 0;
        results.add(new EligibilityResult(loanId, "LOAN_BALANCE", hasBalance,
                hasBalance ? "대출 잔액이 존재합니다." : "대출 잔액이 없어 대환할 수 없습니다."));

        boolean notOverdue = !"Y".equals(loan.getOverdueYn());
        results.add(new EligibilityResult(loanId, "OVERDUE_LOAN", notOverdue,
                notOverdue ? "연체 중이 아닙니다." : "연체 중인 대출은 대환할 수 없습니다."));

        boolean notMatured = loan.getMaturityDate() == null || !loan.getMaturityDate().isBefore(java.time.LocalDate.now());
        results.add(new EligibilityResult(loanId, "LOAN_MATURED", notMatured,
                notMatured ? "만기 도래 전입니다." : "이미 만기가 도래한 대출입니다."));

        return results;
    }
}
