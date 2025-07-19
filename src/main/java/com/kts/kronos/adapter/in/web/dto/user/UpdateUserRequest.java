package com.kts.kronos.adapter.in.web.dto.user;

import jakarta.validation.constraints.Pattern;

import static com.kts.kronos.constants.Messages.INVALID_ROLE;

public record UpdateUserRequest(
        String username,
        String password,
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = INVALID_ROLE)
        String role,
        Boolean enabled
) {
}
