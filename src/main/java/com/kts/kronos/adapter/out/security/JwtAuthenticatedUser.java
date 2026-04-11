package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.enuns.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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
             throw new IllegalArgumentException(JWT_EMPLOYEE_ID_NOT_FOUND);
        }
        return id;
    }

    public UUID getuserId() {
        String token = extractToken();
        UUID id = jwtUtils.getUserIdFromToken(token);
        if (id == null) {
             throw new IllegalArgumentException(JWT_USER_ID_NOT_FOUND);
        }
        return id;
    }

    public String getUsername() {
        String token = extractToken();
        return jwtUtils.getUsernameFromToken(token);
    }

    public Role getCurrentRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalArgumentException("Role atual não encontrada no contexto autenticado.");
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .map(Role::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("Role atual não encontrada no contexto autenticado."));
    }

    public boolean hasAnyRole(Role... roles) {
        var currentRole = getCurrentRole();
        return Arrays.stream(roles).anyMatch(currentRole::equals);
    }

    @Deprecated
    public String getRoleFromToken() {
        return getCurrentRole().name();
    }

    public UUID isWithEmployeeId(UUID employeeId) {
        var userRole = getCurrentRole().name();
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
