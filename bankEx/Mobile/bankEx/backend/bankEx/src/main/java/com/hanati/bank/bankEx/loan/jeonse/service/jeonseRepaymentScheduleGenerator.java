package com.hanati.bank.bankEx.loan.jeonse.service;

import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseContract;
import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseLoanApplication;
import com.hanati.bank.bankEx.loan.jeonse.domain.JeonseRepaymentSchedule;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class jeonseRepaymentScheduleGenerator {

    public LocalDate firstPaymentDate(LocalDate executionDate) {
        return executionDate.plusMonths(1).withDayOfMonth(1);
    }

    public List<JeonseRepaymentSchedule> generate(JeonseLoanApplication application, JeonseContract contract, LocalDate firstPaymentDate) {
        long principal = application.getApprovedAmount();
        double monthlyRate = application.getLoanRate() / 100 / 12;
        long monthlyInterest = Math.round(principal * monthlyRate);

        LocalDate paymentDate = firstPaymentDate;
        int seq = 1;
        LocalDateTime now = LocalDateTime.now();
        List<JeonseRepaymentSchedule> schedules = new ArrayList<>();

        while (!paymentDate.isAfter(contract.getContractEndDate())) {
            boolean isLast = !paymentDate.plusMonths(1).isBefore(contract.getContractEndDate());
            long principalAmount = isLast ? principal : 0L;

            schedules.add(JeonseRepaymentSchedule.builder()
                    .applicationId(application.getApplicationId())
                    .paymentSeq(seq)
                    .paymentDate(paymentDate)
                    .principalAmount(principalAmount)
                    .interestAmount(monthlyInterest)
                    .totalAmount(monthlyInterest + principalAmount)
                    .paidYn("N")
                    .createdAt(now)
                    .build());

            if (isLast) break;
            paymentDate = paymentDate.plusMonths(1);
            seq++;
        }

        return schedules;
    }
}
