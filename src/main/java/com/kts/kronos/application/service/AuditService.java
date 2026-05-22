package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.enuns.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogProvider auditLogProvider;

    public void register(AuditAction action, UUID employeeId, UUID companyId,
                        String resourceType, String resourceId, String riskLevel,
                        String ipAddress, String userAgent, String details) {
        AuditLog log = AuditLog.create(
                employeeId,
                action.name(),
                ipAddress,
                userAgent,
                details,
                companyId,
                resourceType,
                resourceId,
                riskLevel
        );
        auditLogProvider.registerLog(log);
    }

    public void registerLgpd(AuditAction action, UUID employeeId, UUID companyId,
                             String resourceType, String resourceId, String riskLevel,
                             String details, String ipAddress, String userAgent) {
        register(action, employeeId, companyId, resourceType, resourceId, riskLevel, ipAddress, userAgent, details);
    }

    public void registerSecurity(AuditAction action, UUID employeeId, String riskLevel,
                                 String resourceType, String resourceId,
                                 String details, String ipAddress, String userAgent) {
        register(action, employeeId, null, resourceType, resourceId, riskLevel, ipAddress, userAgent, details);
    }

    public List<AuditLog> findByUserId(UUID userId) {
        return auditLogProvider.findByUserId(userId);
    }
}
