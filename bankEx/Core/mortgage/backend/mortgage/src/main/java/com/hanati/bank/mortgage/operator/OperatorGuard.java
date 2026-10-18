package com.hanati.bank.mortgage.operator;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 담당자 권한 검증. 로그인 없이 X-Operator-Id 헤더만 믿는 학습용 모델이다 (Core/refinancing과 같은 방식).
 */
@Component
@RequiredArgsConstructor
public class OperatorGuard {

    private final OperatorRepository operatorRepository;

    public Operator require(String operatorId, OperatorRole role) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATOR_NOT_FOUND));
        if (operator.getRole() != role) {
            throw new BusinessException(ErrorCode.OPERATOR_NOT_AUTHORIZED);
        }
        return operator;
    }
}
