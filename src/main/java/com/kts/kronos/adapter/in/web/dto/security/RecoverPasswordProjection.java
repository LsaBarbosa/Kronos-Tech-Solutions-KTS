package com.kts.kronos.adapter.in.web.dto.security;


import com.kts.kronos.domain.model.enuns.Role;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record RecoverPasswordProjection(
        UUID userId,
        String username,
        String email
) {
}
