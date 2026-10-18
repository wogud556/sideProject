package com.hanati.bank.mortgage.policy.service;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.enums.PolicyType;
import com.hanati.bank.mortgage.policy.repository.MortgagePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * 기준일에 적용할 정책 한 건을 고른다.
 *
 * 1. 기준일이 적용기간 안에 있고, 조건 컬럼마다 값이 같거나 ALL인 행만 후보로 남긴다.
 * 2. 가장 구체적인 행을 고른다. 상품(4) > 차주 유형(2) > 지역(1) 가중치의 합이 큰 쪽이다.
 *    예: 규제지역 생애최초 구입자에게는 "ALL/생애최초"(2)가 "규제지역/ALL"(1)보다 우선한다.
 * 3. 그래도 같으면 버전이 높은 행을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PolicyResolver {

    private final MortgagePolicyRepository policyRepository;

    public MortgagePolicy resolve(PolicyType type, String regionCode, String borrowerType,
                                  String productCode, LocalDate baseDate) {
        return policyRepository.findByPolicyType(type).stream()
                .filter(p -> p.isEffectiveOn(baseDate))
                .filter(p -> matches(p.getRegionCode(), regionCode)
                        && matches(p.getBorrowerType(), borrowerType)
                        && matches(p.getProductCode(), productCode))
                .max(Comparator.comparingInt(PolicyResolver::specificity)
                        .thenComparing(MortgagePolicy::getPolicyVersion))
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }

    private static boolean matches(String policyValue, String actual) {
        return MortgagePolicy.ALL.equals(policyValue) || policyValue.equals(actual);
    }

    private static int specificity(MortgagePolicy p) {
        return (MortgagePolicy.ALL.equals(p.getProductCode()) ? 0 : 4)
                + (MortgagePolicy.ALL.equals(p.getBorrowerType()) ? 0 : 2)
                + (MortgagePolicy.ALL.equals(p.getRegionCode()) ? 0 : 1);
    }
}
