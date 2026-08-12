package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.refinance.dto.ErrorSearchResult;
import com.hanati.bank.refinance.refinance.mapper.RefinanceErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceErrorSearchService {

    private final RefinanceErrorMapper refinanceErrorMapper;

    public List<ErrorSearchResult> search(LocalDate transactionDate, String applicationNo, Long customerId,
                                           String failedStep, String errorCode, String status) {
        return refinanceErrorMapper.search(transactionDate, applicationNo, customerId, failedStep, errorCode, status);
    }
}
