package com.hanati.bank.bankEx.loan.jeonse.service;

import com.hanati.bank.bankEx.loan.jeonse.enums.GuaranteeOrg;
import org.springframework.stereotype.Service;

@Service
public class jeonseRateCalculator {

    private static final double BASE_RATE = 3.50;
    private static final double MIN_RATE = 2.50;
    private static final double MAX_RATE = 8.00;

    public double calculate(int creditScore, double debtRatio, GuaranteeOrg guaranteeOrg,
                             boolean salaryTransferYn, boolean cardUsageYn, boolean autoTransferYn) {
        double rate = BASE_RATE
                + creditScoreAddRate(creditScore)
                + debtRatioAddRate(debtRatio)
                + guaranteeOrgAddRate(guaranteeOrg)
                - preferentialRate(salaryTransferYn, cardUsageYn, autoTransferYn);

        return Math.min(Math.max(rate, MIN_RATE), MAX_RATE);
    }

    private double creditScoreAddRate(int creditScore) {
        if (creditScore >= 900) return 0.30;
        if (creditScore >= 800) return 0.50;
        if (creditScore >= 700) return 0.80;
        return 1.20;
    }

    private double debtRatioAddRate(double debtRatio) {
        if (debtRatio < 0.30) return 0.00;
        if (debtRatio < 0.50) return 0.30;
        return 0.70;
    }

    private double guaranteeOrgAddRate(GuaranteeOrg guaranteeOrg) {
        return switch (guaranteeOrg) {
            case HF -> 0.10;
            case HUG -> 0.20;
            case SGI -> 0.30;
        };
    }

    private double preferentialRate(boolean salaryTransferYn, boolean cardUsageYn, boolean autoTransferYn) {
        double discount = 0.0;
        if (salaryTransferYn) discount += 0.20;
        if (cardUsageYn) discount += 0.10;
        if (autoTransferYn) discount += 0.10;
        return discount;
    }
}
