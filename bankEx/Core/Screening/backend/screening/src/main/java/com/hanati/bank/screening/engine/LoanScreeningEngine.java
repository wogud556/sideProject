package com.hanati.bank.screening.engine;

import com.hanati.bank.screening.entity.CustomerCreditInfo;
import com.hanati.bank.screening.entity.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanScreeningEngine {

    public ScreeningResult screen(CustomerCreditInfo customer, LoanApplication application) {
        int cssScore = 0;

        // 1. 신용점수 평가
        int creditScore = customer.getCreditScore();
        if (creditScore >= 900) {
            cssScore += 40;
        } else if (creditScore >= 800) {
            cssScore += 30;
        } else if (creditScore >= 700) {
            cssScore += 20;
        }

        // 2. 연소득 평가
        long income = customer.getAnnualIncome();
        if (income >= 60_000_000L) {
            cssScore += 25;
        } else if (income >= 40_000_000L) {
            cssScore += 20;
        } else if (income >= 30_000_000L) {
            cssScore += 10;
        }

        // 3. 재직기간 평가
        int months = customer.getEmploymentMonths() != null ? customer.getEmploymentMonths() : 0;
        if (months >= 36) {
            cssScore += 20;
        } else if (months >= 12) {
            cssScore += 10;
        }

        // 4. DSR 계산 (신규 대출 5년 균등 상환 가정)
        double newAnnualRepayment = application.getRequestAmount() / 5.0;
        double totalAnnualRepayment = customer.getAnnualRepayment() + newAnnualRepayment;
        double dsrRate = totalAnnualRepayment / income * 100;

        // 5. DSR 점수
        if (dsrRate <= 40) {
            cssScore += 15;
        } else if (dsrRate <= 50) {
            cssScore += 5;
        }

        // 6. 결과 판단
        String resultStatus;
        String rejectReason = null;
        long approvedAmount = application.getRequestAmount();

        if (creditScore < 700) {
            resultStatus = "REJECTED";
            rejectReason = "신용점수 기준 미달";
            approvedAmount = 0;
        } else if (dsrRate > 50) {
            resultStatus = "REJECTED";
            rejectReason = "DSR 기준 초과";
            approvedAmount = 0;
        } else if (cssScore >= 80 && dsrRate <= 40) {
            resultStatus = "APPROVED";
        } else if (cssScore >= 60 && dsrRate <= 50) {
            resultStatus = "CONDITIONAL";
            approvedAmount = application.getRequestAmount() * 70 / 100;
        } else {
            resultStatus = "REJECTED";
            rejectReason = "CSS 점수 기준 미달";
            approvedAmount = 0;
        }

        return new ScreeningResult(cssScore, dsrRate, approvedAmount, resultStatus, rejectReason);
    }
}
