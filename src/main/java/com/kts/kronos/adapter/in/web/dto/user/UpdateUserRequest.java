package com.kts.kronos.adapter.in.web.dto.user;

import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        String username,
        String password,
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = "Role inválida")
        String role,
        Boolean enabled
) {
}
