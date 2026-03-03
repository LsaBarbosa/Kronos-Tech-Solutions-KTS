package com.kts.kronos.adapter.in.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record LoginResponse(String token) {
    public static LoginResponse empty() {
        return new LoginResponse(null);
    }
}
