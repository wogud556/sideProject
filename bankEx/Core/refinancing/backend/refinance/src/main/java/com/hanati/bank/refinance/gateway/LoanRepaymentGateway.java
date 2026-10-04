package com.hanati.bank.refinance.gateway;

import com.hanati.bank.refinance.gateway.dto.RepaymentInquiryRequest;
import com.hanati.bank.refinance.gateway.dto.RepaymentInquiryResult;
import com.hanati.bank.refinance.gateway.dto.RepaymentRequest;
import com.hanati.bank.refinance.gateway.dto.RepaymentResult;

/**
 * 기존 금융기관(타행 포함) 대출 상환 연계 인터페이스 (명세 16번). Mock 구현체를 반드시 제공한다.
 */
public interface LoanRepaymentGateway {
    RepaymentInquiryResult inquireRepaymentAmount(RepaymentInquiryRequest request);

    RepaymentResult repay(RepaymentRequest request);
}
