package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.AuditLog;

public interface AuditLogProvider {
    void registerLog(AuditLog auditLog);
}