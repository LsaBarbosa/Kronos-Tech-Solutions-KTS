package com.kts.kronos.application.service;

import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.ClientIpResolution;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRequestContextService {

    private final ClientIpResolver clientIpResolver;

    public AuditRequestContext extractContext() {
        try {
            var requestAttrs = RequestContextHolder.getRequestAttributes();
            if (requestAttrs instanceof ServletRequestAttributes servletAttrs) {
                return extractFromRequest(servletAttrs.getRequest());
            }
        } catch (Exception e) {
            log.debug("Falha ao extrair contexto de auditoria da request", e);
        }
        return AuditRequestContext.unknown();
    }

    private AuditRequestContext extractFromRequest(HttpServletRequest request) {
        ClientIpResolution ipResolution = clientIpResolver.resolveWithDetails(request);
        String userAgent = extractUserAgent(request);
        return new AuditRequestContext(
            ipResolution.ipAddress(),
            userAgent,
            ipResolution.source(),
            ipResolution.trusted()
        );
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "Desconhecido" : userAgent;
    }

    public record AuditRequestContext(
        String ipAddress,
        String userAgent,
        String ipSource,
        boolean ipTrusted
    ) {
        public static AuditRequestContext unknown() {
            return new AuditRequestContext("unknown", "Desconhecido", "UNKNOWN", false);
        }
    }
}
