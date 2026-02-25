package com.kts.kronos.adapter.in.web.dto.security;


import com.kts.kronos.domain.model.enuns.Role;

import java.util.UUID;

public record RecoverPasswordProjection(
        UUID userId,
        String username,
        String email
) {
}
