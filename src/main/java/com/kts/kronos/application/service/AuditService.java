package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.ClientIpResolution;
import com.kts.kronos.domain.model.enuns.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogProvider auditLogProvider;
    private final ObjectMapper objectMapper;

    public void register(AuditAction action, UUID employeeId, UUID companyId,
                        String resourceType, String resourceId, String riskLevel,
                        String ipAddress, String userAgent, String details) {
        String safeDetails = SensitiveDataMasker.sanitizeDetails(details);
        AuditLog log = AuditLog.create(
                employeeId,
                action.name(),
                ipAddress,
                userAgent,
                safeDetails,
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

    public void registerLgpd(AuditAction action, UUID employeeId, UUID companyId,
                             String resourceType, String resourceId, String riskLevel,
                             String details, ClientIpResolution ipResolution, String userAgent) {
        String enrichedDetails = enrichDetailsWithIpMetadata(details, ipResolution);
        register(action, employeeId, companyId, resourceType, resourceId, riskLevel,
                ipResolution.ipAddress(), userAgent, enrichedDetails);
    }

    public void registerSecurity(AuditAction action, UUID employeeId, String riskLevel,
                                 String resourceType, String resourceId,
                                 String details, String ipAddress, String userAgent) {
        register(action, employeeId, null, resourceType, resourceId, riskLevel, ipAddress, userAgent, details);
    }

    public void registerSecurity(AuditAction action, UUID employeeId, String riskLevel,
                                 String resourceType, String resourceId,
                                 String details, ClientIpResolution ipResolution, String userAgent) {
        String enrichedDetails = enrichDetailsWithIpMetadata(details, ipResolution);
        register(action, employeeId, null, resourceType, resourceId, riskLevel,
                ipResolution.ipAddress(), userAgent, enrichedDetails);
    }

    private String enrichDetailsWithIpMetadata(String details, ClientIpResolution ipResolution) {
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            if (details != null && !details.isBlank()) {
                detailsMap = objectMapper.readValue(details, Map.class);
            }
            detailsMap.put("ipSource", ipResolution.source());
            detailsMap.put("ipTrusted", ipResolution.trusted());
            return objectMapper.writeValueAsString(detailsMap);
        } catch (Exception e) {
            String baseDetails = details != null ? details : "";
            return baseDetails + " | ipSource=" + ipResolution.source() + ",ipTrusted=" + ipResolution.trusted();
        }
    }

    public void registerRetentionAudit(AuditAction action, String resourceType, String details) {
        register(action, null, null, resourceType, null, "SYSTEM", null, null, details);
    }

    public List<AuditLog> findByUserId(UUID userId) {
        return auditLogProvider.findByUserId(userId);
    }
}
