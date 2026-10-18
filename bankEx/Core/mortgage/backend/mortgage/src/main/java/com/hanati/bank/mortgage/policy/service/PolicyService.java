package com.hanati.bank.mortgage.policy.service;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.operator.OperatorGuard;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.policy.dto.PolicyCreateRequest;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.repository.MortgagePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final MortgagePolicyRepository policyRepository;
    private final OperatorGuard operatorGuard;

    public List<MortgagePolicy> getPolicies() {
        return policyRepository.findAllByOrderByPolicyTypeAscPolicyIdAsc();
    }

    /**
     * 새 정책 버전 등록. 같은 조건의 기존 정책 중 새 시작일에도 유효한 것은 전날로 종료한다.
     * 기존 행의 값은 바꾸지 않으므로, 과거 한도 산정 결과는 당시 정책 ID로 그대로 재현된다.
     */
    @Transactional
    public MortgagePolicy create(String operatorId, PolicyCreateRequest req) {
        operatorGuard.require(operatorId, OperatorRole.APPROVER);
        if (req.effectiveTo() != null && req.effectiveTo().isBefore(req.effectiveFrom())) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_PERIOD);
        }
        MortgagePolicy candidate = MortgagePolicy.builder()
                .policyType(req.policyType())
                .regionCode(req.regionCode())
                .borrowerType(req.borrowerType())
                .productCode(req.productCode())
                .build();

        List<MortgagePolicy> sameKey = policyRepository.findByPolicyType(req.policyType()).stream()
                .filter(p -> p.sameKeyAs(candidate))
                .toList();
        sameKey.stream()
                .filter(p -> p.isEffectiveOn(req.effectiveFrom()))
                .forEach(p -> p.closeAt(req.effectiveFrom().minusDays(1)));
        int nextVersion = sameKey.stream().mapToInt(MortgagePolicy::getPolicyVersion).max().orElse(0) + 1;

        return policyRepository.save(MortgagePolicy.builder()
                .policyType(req.policyType())
                .regionCode(req.regionCode())
                .borrowerType(req.borrowerType())
                .productCode(req.productCode())
                .policyValue(req.policyValue())
                .effectiveFrom(req.effectiveFrom())
                .effectiveTo(req.effectiveTo())
                .policyVersion(nextVersion)
                .basis(req.basis())
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
