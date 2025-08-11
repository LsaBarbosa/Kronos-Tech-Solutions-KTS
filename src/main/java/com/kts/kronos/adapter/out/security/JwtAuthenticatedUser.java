package com.kts.kronos.adapter.out.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.UUID;

@Component
public class JwtAuthenticatedUser {

    public UUID getEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof SecurityUser)) {
            throw new IllegalStateException("Usuário não autenticado ou detalhes de usuário inválidos.");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        return securityUser.getEmployeeId();
    }

    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuário não autenticado.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
