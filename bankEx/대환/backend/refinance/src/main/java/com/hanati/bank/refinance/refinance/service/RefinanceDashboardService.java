package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.dto.DashboardResponse;
import com.hanati.bank.refinance.refinance.entity.RefinanceApplication;
import com.hanati.bank.refinance.refinance.repository.RefinanceApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceDashboardService {

    private final RefinanceApplicationRepository refinanceApplicationRepository;

    public DashboardResponse getDashboard() {
        List<RefinanceApplication> all = refinanceApplicationRepository.findAll();
        LocalDate today = LocalDate.now();

        long todayApplicationCount = all.stream()
                .filter(a -> a.getApplicationDate() != null && a.getApplicationDate().toLocalDate().isEqual(today))
                .count();
        long reviewingCount = countByStatus(all, RefinanceStatus.REQUESTED) + countByStatus(all, RefinanceStatus.REVIEWING);
        long approvedCount = countByStatus(all, RefinanceStatus.APPROVED)
                + countByStatus(all, RefinanceStatus.EXECUTING)
                + countByStatus(all, RefinanceStatus.NEW_LOAN_EXECUTED)
                + countByStatus(all, RefinanceStatus.REPAYING)
                + countByStatus(all, RefinanceStatus.COMPLETED);
        long executionPendingCount = countByStatus(all, RefinanceStatus.APPROVED);
        long completedCount = countByStatus(all, RefinanceStatus.COMPLETED);
        long failedCount = countByStatus(all, RefinanceStatus.FAILED);

        return new DashboardResponse(todayApplicationCount, reviewingCount, approvedCount,
                executionPendingCount, completedCount, failedCount);
    }

    private long countByStatus(List<RefinanceApplication> all, RefinanceStatus status) {
        return all.stream().filter(a -> a.getStatus() == status).count();
    }
}
