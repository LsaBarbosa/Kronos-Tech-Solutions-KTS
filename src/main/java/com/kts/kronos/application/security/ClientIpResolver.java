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
            return ClientIpResolution.of("unknown", ClientIpResolution.IpSource.UNKNOWN, false, false);
        }

        String remoteAddr = request.getRemoteAddr();
        boolean isProxyTrusted = IpAddressValidator.isTrustedProxy(remoteAddr, properties.getTrustedProxyCidrs());

        if (!properties.isTrustForwardedHeaders() || !isProxyTrusted) {
            return resolveDirect(remoteAddr);
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            if (hops.length > 0 && !hops[0].trim().isBlank()) {
                String firstHop = hops[0].trim();
                // Validate proxy chain: all hops except first should be trusted proxies
                boolean chainValid = validateProxyChain(hops);
                return ClientIpResolution.of(firstHop, ClientIpResolution.IpSource.X_FORWARDED_FOR, true, chainValid);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return ClientIpResolution.of(realIp.trim(), ClientIpResolution.IpSource.X_REAL_IP, true, true);
        }

        return resolveDirect(remoteAddr);
    }

    private boolean validateProxyChain(String[] hops) {
        // All hops after the first one should be trusted proxies
        // hops[1..n] should all be trusted, and the last one should match remoteAddr
        for (int i = 1; i < hops.length; i++) {
            String hop = hops[i].trim();
            if (!IpAddressValidator.isTrustedProxy(hop, properties.getTrustedProxyCidrs())) {
                return false;
            }
        }
        return true;
    }

    private ClientIpResolution resolveDirect(String remoteAddr) {
        String ipAddress = remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
        boolean trusted = remoteAddr != null && !remoteAddr.isBlank() && !remoteAddr.equals("unknown");
        return ClientIpResolution.of(ipAddress, ClientIpResolution.IpSource.REMOTE_ADDR, trusted, true);
    }
}
