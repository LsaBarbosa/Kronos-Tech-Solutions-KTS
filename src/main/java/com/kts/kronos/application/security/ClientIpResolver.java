package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.domain.model.ClientIpResolution;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {
    private final ClientIpResolverProperties properties;

    public String resolve(HttpServletRequest request) {
        ClientIpResolution resolution = resolveWithDetails(request);
        return resolution.ipAddress();
    }

    public ClientIpResolution resolveWithDetails(HttpServletRequest request) {
        if (request == null) {
            return ClientIpResolution.of("unknown", ClientIpResolution.IpSource.UNKNOWN, false);
        }

        String remoteAddr = request.getRemoteAddr();
        boolean isProxyTrusted = IpAddressValidator.isTrustedProxy(remoteAddr, properties.getTrustedProxyCidrs());

        if (!properties.isTrustForwardedHeaders() || !isProxyTrusted) {
            return resolveDirect(remoteAddr);
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstHop = forwardedFor.split(",", 2)[0].trim();
            if (!firstHop.isBlank()) {
                return ClientIpResolution.of(firstHop, ClientIpResolution.IpSource.X_FORWARDED_FOR, true);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return ClientIpResolution.of(realIp.trim(), ClientIpResolution.IpSource.X_REAL_IP, true);
        }

        return resolveDirect(remoteAddr);
    }

    private ClientIpResolution resolveDirect(String remoteAddr) {
        String ipAddress = remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
        boolean trusted = remoteAddr != null && !remoteAddr.isBlank() && !remoteAddr.equals("unknown");
        return ClientIpResolution.of(ipAddress, ClientIpResolution.IpSource.REMOTE_ADDR, trusted);
    }
}
