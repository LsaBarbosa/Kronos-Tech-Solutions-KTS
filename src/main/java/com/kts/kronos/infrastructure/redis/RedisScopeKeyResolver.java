package com.kts.kronos.infrastructure.redis;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RedisScopeKeyResolver {
    private RedisScopeKeyResolver() {
    }

    public static String authenticatedScope(String... parts) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "anonymous"
                : authentication.getName().trim();
        String roles = authentication == null
                ? "anonymous"
                : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(","));
        return join(principal, roles, parts);
    }

    public static String publicScope(String... parts) {
        return join("public", "anonymous", parts);
    }

    private static String join(String principal, String roles, String... parts) {
        List<String> segments = new java.util.ArrayList<>();
        segments.add(principal);
        segments.add(roles);
        if (parts != null && parts.length > 0) {
            segments.addAll(Arrays.stream(parts)
                    .map(value -> value == null || value.isBlank() ? "unknown" : value.trim())
                    .toList());
        }
        return String.join("|", segments);
    }
}
