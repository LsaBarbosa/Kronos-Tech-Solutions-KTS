package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogProvider {
    void registerLog(AuditLog auditLog);

    List<AuditLog> findByUserId(UUID userId);
}
