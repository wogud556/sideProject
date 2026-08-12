package com.hanati.bank.refinance.refinance.dto;

import java.time.LocalDateTime;

public class ErrorSearchResult {
    private Long errorId;
    private Long applicationId;
    private String applicationNo;
    private Long customerId;
    private String failedStep;
    private String errorCode;
    private String errorMessage;
    private String status;
    private Integer retryCount;
    private LocalDateTime createdAt;

    public Long getErrorId() { return errorId; }
    public void setErrorId(Long errorId) { this.errorId = errorId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getFailedStep() { return failedStep; }
    public void setFailedStep(String failedStep) { this.failedStep = failedStep; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
