package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

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
            throw new IllegalArgumentException(JWT_EMPLOYEE_ID_NOT_FOUND);
        }
        return id;
    }

    public UUID getuserId() {
        String token = extractToken();
        UUID id = jwtUtils.getUserIdFromToken(token);
        if (id == null) {
            // token sem claim ou inválido => tratamos como 401/400 de forma clara
            throw new IllegalArgumentException(JWT_USER_ID_NOT_FOUND);
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
    public UUID isWithEmployeeId(UUID employeeId) {
        var userRole = getRoleFromToken();
        var loggedInEmployeeId = getEmployeeId();

        return switch (userRole) {
            case "PARTNER" -> loggedInEmployeeId;
            case "MANAGER" -> (employeeId != null) ? employeeId : loggedInEmployeeId;
            default ->
                    (employeeId != null) ? employeeId : loggedInEmployeeId;
        };
    }

    private String extractToken() {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        throw new IllegalArgumentException(HEADER_AUTHORIZATION_NOT_FOUND);
    }
}
