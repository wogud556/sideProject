package com.hanati.bank.refinance.audit.repository;

import com.hanati.bank.refinance.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
