package com.kts.kronos.domain.model;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AuditLog(
        UUID id,
        UUID actorUserId,
        UUID targetEmployeeId,
        String action,
        String ipAddress,
        String userAgent,
        String details,
        LocalDateTime timestamp,
        UUID companyId,
        String resourceType,
        String resourceId,
        String correlationId,
        String riskLevel
) {
    public static AuditLog create(UUID actorUserId, String action, String ipAddress, String userAgent, String details) {
        return AuditLog.builder()
                .actorUserId(actorUserId)
                .action(action)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AuditLog create(UUID actorUserId, UUID targetEmployeeId, String action,
                                  String ipAddress, String userAgent, String details,
                                  UUID companyId, String resourceType, String resourceId, String riskLevel) {
        return AuditLog.builder()
                .actorUserId(actorUserId)
                .targetEmployeeId(targetEmployeeId)
                .action(action)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .timestamp(LocalDateTime.now())
                .companyId(companyId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .riskLevel(riskLevel)
                .build();
    }
}
