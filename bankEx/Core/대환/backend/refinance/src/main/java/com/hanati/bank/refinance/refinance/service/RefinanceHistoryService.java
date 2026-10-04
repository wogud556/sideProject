package com.hanati.bank.refinance.refinance.service;

import com.hanati.bank.refinance.refinance.dto.RefinanceHistoryResponse;
import com.hanati.bank.refinance.refinance.repository.RefinanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefinanceHistoryService {

    private final RefinanceHistoryRepository refinanceHistoryRepository;

    public List<RefinanceHistoryResponse> getHistory(Long applicationId) {
        return refinanceHistoryRepository.findByApplicationIdOrderByProcessedAtAsc(applicationId).stream()
                .map(RefinanceHistoryResponse::new)
                .toList();
    }
}
