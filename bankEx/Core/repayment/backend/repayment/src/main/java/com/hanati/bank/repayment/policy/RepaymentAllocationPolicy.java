package com.hanati.bank.repayment.policy;

/**
 * 상환금 배분 정책 (명세 4번). 배분 순서를 서비스 코드에 직접 작성하지 않기 위한 확장점이며,
 * 상품 또는 상품 유형에 따라 {@link RepaymentAllocationPolicySelector}가 구현체를 고른다.
 */
public interface RepaymentAllocationPolicy {

    RepaymentAllocationResult allocate(RepaymentAllocationContext context);

    /** 이 정책이 담당하는 상품 유형. */
    String supportedProductType();
}
