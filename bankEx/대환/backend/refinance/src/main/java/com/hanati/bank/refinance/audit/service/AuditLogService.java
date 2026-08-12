package com.hanati.bank.refinance.audit.service;

import com.hanati.bank.refinance.audit.entity.AuditLog;
import com.hanati.bank.refinance.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 사용자 주요 업무 행위에 대한 Audit Log. 일반 Application Log와 분리된 별도 테이블(TB_AUDIT_LOG)에 기록한다 (명세 21번).
 * 각 Service가 주요 행위 직후 명시적으로 호출한다 (AOP 없이 bankEx 스타일 그대로).
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(String operatorId, String actionType, Long customerId, Long applicationId, String description) {
        auditLogRepository.save(AuditLog.builder()
                .operatorId(operatorId)
                .actionType(actionType)
                .customerId(customerId)
                .applicationId(applicationId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
