package com.hanati.bank.screening.dto;

import com.hanati.bank.screening.entity.LoanProduct;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class LoanProductResponse {
    private Long productId;
    private String productName;
    private String productType;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private Long maxLimitAmount;
    private Integer loanPeriodMonths;

    public LoanProductResponse(LoanProduct p) {
        this.productId = p.getProductId();
        this.productName = p.getProductName();
        this.productType = p.getProductType();
        this.minInterestRate = p.getMinInterestRate();
        this.maxInterestRate = p.getMaxInterestRate();
        this.maxLimitAmount = p.getMaxLimitAmount();
        this.loanPeriodMonths = p.getLoanPeriodMonths();
    }
}
