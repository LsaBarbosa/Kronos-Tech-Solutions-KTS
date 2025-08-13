package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticatedUser {
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    public UUID getEmployeeId() {
        String token = extractToken();
        UUID id = jwtUtils.getEmployeeIdFromToken(token);
        if (id == null) {
            // token sem claim ou inválido => tratamos como 401/400 de forma clara
            throw new IllegalArgumentException("JWT sem employeeId.");
        }
        return id;
    }

    public String getUsername() {
        String token = extractToken();
        return jwtUtils.getUsernameFromToken(token);
    }
    public String getRoleFromToken() {
        String token = extractToken();
        return jwtUtils.getRoleFromToken(token);
    }

    private String extractToken() {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        throw new IllegalArgumentException("Token JWT não encontrado no header Authorization.");
    }
}
