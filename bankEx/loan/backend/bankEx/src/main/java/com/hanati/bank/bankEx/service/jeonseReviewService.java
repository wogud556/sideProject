package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.entity.JeonseContract;
import com.hanati.bank.bankEx.entity.JeonseLoanApplication;
import com.hanati.bank.bankEx.enums.RejectReasonCode;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class jeonseReviewService {

    public Optional<RejectReasonCode> reviewGuarantee(JeonseLoanApplication application, JeonseContract contract, double debtRatio) {
        long seniorClaim = contract.getSeniorClaimAmount() == null ? 0L : contract.getSeniorClaimAmount();

        return switch (application.getGuaranteeOrg()) {
            case HF -> application.getApprovedAmount() > Math.round(contract.getDepositAmount() * 0.8)
                    ? Optional.of(RejectReasonCode.GUARANTEE_REJECTED)
                    : Optional.empty();
            case HUG -> seniorClaim + application.getApprovedAmount() > Math.round(contract.getDepositAmount() * 0.9)
                    ? Optional.of(RejectReasonCode.GUARANTEE_REJECTED)
                    : Optional.empty();
            case SGI -> application.getCreditScore() < 700 || debtRatio >= 0.50
                    ? Optional.of(RejectReasonCode.GUARANTEE_REJECTED)
                    : Optional.empty();
        };
    }

    public Optional<RejectReasonCode> reviewBank(JeonseLoanApplication application) {
        if (application.getApprovedAmount() == null || application.getApprovedAmount() <= 0) {
            return Optional.of(RejectReasonCode.BANK_REJECTED);
        }
        return Optional.empty();
    }
}
