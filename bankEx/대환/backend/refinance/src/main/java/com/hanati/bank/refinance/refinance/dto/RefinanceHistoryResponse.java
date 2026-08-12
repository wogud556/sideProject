package com.hanati.bank.refinance.refinance.dto;

import com.hanati.bank.refinance.refinance.domain.RefinanceStatus;
import com.hanati.bank.refinance.refinance.entity.RefinanceHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RefinanceHistoryResponse {
    private final Long historyId;
    private final String actionType;
    private final RefinanceStatus fromStatus;
    private final RefinanceStatus toStatus;
    private final String description;
    private final String processedBy;
    private final LocalDateTime processedAt;

    public RefinanceHistoryResponse(RefinanceHistory history) {
        this.historyId = history.getHistoryId();
        this.actionType = history.getActionType();
        this.fromStatus = history.getFromStatus();
        this.toStatus = history.getToStatus();
        this.description = history.getDescription();
        this.processedBy = history.getProcessedBy();
        this.processedAt = history.getProcessedAt();
    }
}
