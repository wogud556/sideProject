package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.dto.LoanProductResponse;
import com.hanati.bank.bankEx.entity.LoanApplication;
import com.hanati.bank.bankEx.entity.LoanProduct;
import com.hanati.bank.bankEx.repository.LoanApplicationRepository;
import com.hanati.bank.bankEx.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class loanService {

    private final LoanProductRepository loanProductRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    public List<LoanProductResponse> getProducts() {
        return loanProductRepository.findAll().stream()
                .map(p -> new LoanProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getInterestRate(),
                        p.getMaxLimit(),
                        p.getDescription()
                ))
                .collect(Collectors.toList());
    }

    public LoanProductResponse getProduct(Long productId) {
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
        return new LoanProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getInterestRate(),
                product.getMaxLimit(),
                product.getDescription()
        );
    }

    @Transactional
    public LoanApplicationResponse applyLoan(LoanApplicationRequest request) {
        LoanProduct product = loanProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));

        if (request.getRequestAmount() > product.getMaxLimit()) {
            throw new IllegalArgumentException("신청 금액이 한도를 초과합니다");
        }

        LoanApplication application = LoanApplication.builder()
                .userId(request.getUserId())
                .accountNumber(request.getAccountNumber())
                .productId(request.getProductId())
                .requestAmount(request.getRequestAmount())
                .loanPeriod(request.getLoanPeriod())
                .status("심사중")
                .createdAt(LocalDateTime.now())
                .build();
        LoanApplication saved = loanApplicationRepository.save(application);

        return new LoanApplicationResponse(
                saved.getApplicationId(),
                product.getProductName(),
                saved.getRequestAmount(),
                saved.getLoanPeriod(),
                saved.getStatus(),
                saved.getCreatedAt().toString()
        );
    }

    public List<LoanApplicationResponse> getMyApplications(String userId) {
        return loanApplicationRepository.findByUserId(userId).stream()
                .map(a -> {
                    String productName = loanProductRepository.findById(a.getProductId())
                            .map(LoanProduct::getProductName)
                            .orElse("알 수 없음");
                    return new LoanApplicationResponse(
                            a.getApplicationId(),
                            productName,
                            a.getRequestAmount(),
                            a.getLoanPeriod(),
                            a.getStatus(),
                            a.getCreatedAt().toString()
                    );
                })
                .collect(Collectors.toList());
    }
}
