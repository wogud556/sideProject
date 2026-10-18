package com.hanati.bank.mortgage.application.service;

import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageDebt;
import com.hanati.bank.mortgage.application.entity.MortgageIncome;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.repository.MortgageApplicationRepository;
import com.hanati.bank.mortgage.application.repository.MortgageDebtRepository;
import com.hanati.bank.mortgage.application.repository.MortgageIncomeRepository;
import com.hanati.bank.mortgage.application.repository.MortgagePropertyRepository;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.product.MortgageProduct;
import com.hanati.bank.mortgage.product.MortgageProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 단계별 서비스가 공통으로 쓰는 신청 데이터 조회. 없으면 업무 예외로 바꿔 던진다. */
@Component
@RequiredArgsConstructor
public class MortgageApplicationLoader {

    private final MortgageApplicationRepository applicationRepository;
    private final MortgagePropertyRepository propertyRepository;
    private final MortgageIncomeRepository incomeRepository;
    private final MortgageDebtRepository debtRepository;
    private final MortgageProductRepository productRepository;

    public MortgageApplication get(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    /** 상태를 바꾸기 전에 비관적 잠금으로 가져온다 */
    public MortgageApplication lock(Long applicationId) {
        return applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    /** 고객 요청용. 본인 신청 건이 아니면 거부한다 */
    public MortgageApplication lockOwned(Long applicationId, String customerId) {
        MortgageApplication application = lock(applicationId);
        if (!application.getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_OWNED);
        }
        return application;
    }

    public MortgageProduct product(String productCode) {
        return productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public MortgageProperty property(Long applicationId) {
        return propertyRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    public MortgageIncome income(Long applicationId) {
        return incomeRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    public List<MortgageDebt> debts(Long applicationId) {
        return debtRepository.findByApplicationId(applicationId);
    }
}
