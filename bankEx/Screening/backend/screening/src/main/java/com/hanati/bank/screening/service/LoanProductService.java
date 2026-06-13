package com.hanati.bank.screening.service;

import com.hanati.bank.screening.dto.LoanProductResponse;
import com.hanati.bank.screening.entity.LoanProduct;
import com.hanati.bank.screening.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public List<LoanProductResponse> getActiveProducts() {
        return loanProductRepository.findByUseYn("Y").stream()
                .map(LoanProductResponse::new)
                .toList();
    }

    public LoanProductResponse getProduct(Long productId) {
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return new LoanProductResponse(product);
    }
}
