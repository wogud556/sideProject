package com.hanati.bank.repayment.policy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 대출계좌의 상품 유형으로 배분 정책을 고른다 (명세 4번).
 *
 * <p>구현체를 Spring 빈으로 추가하고 {@link RepaymentAllocationPolicy#supportedProductType()}만
 * 선언하면 자동으로 등록된다. 등록되지 않은 유형은 기본 정책으로 처리한다.
 * GeneralLoan / JeonseLoan / DelinquentLoan 전용 정책은 아직 추가하지 않았다.
 */
@Component
public class RepaymentAllocationPolicySelector {

    private final Map<String, RepaymentAllocationPolicy> policies;
    private final RepaymentAllocationPolicy defaultPolicy;

    public RepaymentAllocationPolicySelector(List<RepaymentAllocationPolicy> policies,
                                              DefaultRepaymentAllocationPolicy defaultPolicy) {
        this.policies = policies.stream().collect(Collectors.toMap(
                RepaymentAllocationPolicy::supportedProductType, Function.identity()));
        this.defaultPolicy = defaultPolicy;
    }

    public RepaymentAllocationPolicy select(String productType) {
        if (productType == null) {
            return defaultPolicy;
        }
        return policies.getOrDefault(productType, defaultPolicy);
    }
}
