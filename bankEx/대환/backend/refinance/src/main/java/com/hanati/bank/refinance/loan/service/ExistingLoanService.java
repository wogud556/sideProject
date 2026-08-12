package com.hanati.bank.refinance.loan.service;

import com.hanati.bank.refinance.audit.service.AuditLogService;
import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.loan.dto.ExistingLoanResponse;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExistingLoanService {

    private final ExistingLoanRepository existingLoanRepository;
    private final AuditLogService auditLogService;

    public List<ExistingLoanResponse> getByCustomer(Long customerId, String operatorId) {
        List<ExistingLoanResponse> results = existingLoanRepository.findByCustomerId(customerId).stream()
                .map(ExistingLoanResponse::new)
                .toList();
        auditLogService.record(operatorId, "대출조회", customerId, null, "고객 대출현황 조회 (" + results.size() + "건)");
        return results;
    }

    public ExistingLoanResponse get(Long loanId, String operatorId) {
        ExistingLoan loan = getLoanOrThrow(loanId);
        auditLogService.record(operatorId, "대출조회", loan.getCustomerId(), null, "대출 상세 조회 (loanId=" + loanId + ")");
        return new ExistingLoanResponse(loan);
    }

    public ExistingLoan getLoanOrThrow(Long loanId) {
        return existingLoanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
    }
}
