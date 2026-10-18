package com.hanati.bank.mortgage.application.service;

import com.hanati.bank.mortgage.application.dto.ApplicationCreateRequest;
import com.hanati.bank.mortgage.application.entity.MortgageApplication;
import com.hanati.bank.mortgage.application.entity.MortgageDebt;
import com.hanati.bank.mortgage.application.entity.MortgageIncome;
import com.hanati.bank.mortgage.application.entity.MortgageProperty;
import com.hanati.bank.mortgage.application.enums.ApplicationStatus;
import com.hanati.bank.mortgage.application.repository.MortgageApplicationRepository;
import com.hanati.bank.mortgage.application.repository.MortgageDebtRepository;
import com.hanati.bank.mortgage.application.repository.MortgageIncomeRepository;
import com.hanati.bank.mortgage.application.repository.MortgagePropertyRepository;
import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.common.util.MortgageNoGenerator;
import com.hanati.bank.mortgage.product.MortgageProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MortgageApplicationService {

    private final MortgageApplicationLoader loader;
    private final MortgageApplicationRepository applicationRepository;
    private final MortgagePropertyRepository propertyRepository;
    private final MortgageIncomeRepository incomeRepository;
    private final MortgageDebtRepository debtRepository;

    /** 신청 접수. 신청 원장·담보 주택·소득·기존 부채를 한 번에 저장한다. */
    @Transactional
    public MortgageApplication apply(ApplicationCreateRequest req, LocalDate today) {
        MortgageProduct product = loader.product(req.productCode());
        if (req.requestedAmount() < product.getMinAmount() || req.requestedAmount() > product.getMaxAmount()) {
            throw new BusinessException(ErrorCode.AMOUNT_OUT_OF_PRODUCT_RANGE);
        }
        if (req.termMonths() < product.getMinTermMonths() || req.termMonths() > product.getMaxTermMonths()) {
            throw new BusinessException(ErrorCode.TERM_OUT_OF_PRODUCT_RANGE);
        }

        LocalDateTime now = LocalDateTime.now();
        MortgageApplication application = applicationRepository.save(MortgageApplication.builder()
                .customerId(req.customerId())
                .customerName(req.customerName())
                .productCode(product.getProductCode())
                .requestedAmount(req.requestedAmount())
                .termMonths(req.termMonths())
                .repaymentMethod(req.repaymentMethod())
                .loanPurpose(req.loanPurpose())
                .houseCount(req.houseCount())
                .firstHomeBuyer(req.firstHomeBuyer())
                .status(ApplicationStatus.APPLIED)
                .appliedAt(now)
                .updatedAt(now)
                .build());
        Long id = application.getApplicationId();
        application.assignApplicationNo(MortgageNoGenerator.applicationNo(today, id));

        ApplicationCreateRequest.Property p = req.property();
        propertyRepository.save(MortgageProperty.builder()
                .applicationId(id)
                .address(p.address())
                .regionCode(p.regionCode())
                .propertyType(p.propertyType())
                .exclusiveArea(p.exclusiveArea())
                .declaredPrice(p.declaredPrice())
                .build());
        incomeRepository.save(MortgageIncome.builder()
                .applicationId(id)
                .incomeType(req.income().incomeType())
                .annualIncome(req.income().annualIncome())
                .employerName(req.income().employerName())
                .build());
        req.debts().forEach(d -> debtRepository.save(MortgageDebt.builder()
                .applicationId(id)
                .debtType(d.debtType())
                .institution(d.institution())
                .balance(d.balance())
                .annualPrincipal(d.annualPrincipal())
                .annualInterest(d.annualInterest())
                .build()));
        return application;
    }

    /** 고객 철회. 실행 전 단계에서만 가능하다 (전이 규칙이 막는다). */
    @Transactional
    public void withdraw(Long applicationId, String customerId) {
        loader.lockOwned(applicationId, customerId).changeStatus(ApplicationStatus.WITHDRAWN, LocalDateTime.now());
    }
}
