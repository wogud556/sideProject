package com.hanati.bank.refinance.operator.service;

import com.hanati.bank.refinance.common.exception.BusinessException;
import com.hanati.bank.refinance.common.exception.ErrorCode;
import com.hanati.bank.refinance.operator.entity.Operator;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 경량 권한 검증. 실제 로그인/세션/JWT가 아니라 X-Operator-Id 헤더로 넘어온 직원 ID를 조회해
 * role만 확인한다. bankEx 흡수 시 Spring Security 인증으로 교체 필요.
 */
@Service
@RequiredArgsConstructor
public class OperatorAuthService {

    private final OperatorRepository operatorRepository;

    public Operator requireRole(String operatorId, OperatorRole... allowedRoles) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new BusinessException(ErrorCode.OPERATOR_NOT_FOUND);
        }
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATOR_NOT_FOUND));

        Set<OperatorRole> allowed = Set.of(allowedRoles);
        if (operator.getRole() == OperatorRole.ROLE_ADMIN) {
            return operator;
        }
        if (!allowed.contains(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ROLE);
        }
        return operator;
    }

    public Operator get(String operatorId) {
        return operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATOR_NOT_FOUND));
    }
}
