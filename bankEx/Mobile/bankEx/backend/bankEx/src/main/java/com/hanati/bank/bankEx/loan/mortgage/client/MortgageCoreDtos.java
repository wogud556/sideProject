package com.hanati.bank.bankEx.loan.mortgage.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core/mortgage 응답 중 bankEx가 쓰는 필드만 받는 타입.
 * Core 응답에는 한도 산정 근거·정책 ID 같은 내부 정보가 더 있지만 여기서 버린다.
 * 날짜·시각은 Core가 보낸 ISO 문자열 그대로 화면에 넘긴다.
 */
public final class MortgageCoreDtos {

    private MortgageCoreDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(String productCode, String productName, Long minAmount, Long maxAmount,
                          Integer minTermMonths, Integer maxTermMonths, BigDecimal baseRate, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationSummary(Long applicationId, String applicationNo, String customerId, String productCode,
                                     Long requestedAmount, String status, String statusLabel, String appliedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationDetail(ApplicationSummary application, String repaymentMethod, Integer termMonths,
                                    Property property, Approval approval, Agreement agreement, Long loanAccountId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Property(String address, String regionCode, String propertyType, Long declaredPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Approval(Long approvedAmount, BigDecimal interestRate, Integer termMonths, String repaymentMethod,
                           String conditions, String expiresOn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Agreement(String agreementNo, String disbursementAccountNo, Long agreedAmount, String agreedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanAccount(Account account, String statusLabel, String repaymentMethodLabel,
                              Installment nextInstallment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(Long loanAccountId, String loanAccountNo, String customerId, String productCode,
                          Long principal, Long outstandingPrincipal, BigDecimal interestRate, Integer termMonths,
                          String repaymentMethod, String executionDate, String maturityDate, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Installment(Integer installmentNo, String dueDate, Long principal, Long interest,
                              Long totalPayment, Long balanceAfter, String status, String paidAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repayment(Long repaymentId, Integer installmentNo, Long principalPaid, Long interestPaid,
                            Long totalPaid, Long outstandingAfter, String paidAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String code, String message) {
    }

    /** 고객 이름은 bankEx 회원 정보에서 채워 보낸다 */
    public record ApplyCommand(String customerId, String customerName, String productCode, Long requestedAmount,
                               Integer termMonths, String repaymentMethod, String loanPurpose, Integer houseCount,
                               Boolean firstHomeBuyer, Object property, Object income, List<?> debts) {
    }
}
