package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.deposit.savings.dto.SavingsProductResponse;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class savingsProductService {

    private final SavingsProductMapper savingsProductMapper;

    public List<SavingsProductResponse> getProducts() {
        return savingsProductMapper.findAllOnSale().stream()
                .map(p -> new SavingsProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getBaseRate(),
                        p.getMaxRate(),
                        p.getMinAmount(),
                        p.getMaxAmount(),
                        p.getPeriod(),
                        p.getAutoTransferYn(),
                        p.getStatus().name()
                ))
                .collect(Collectors.toList());
    }
}
