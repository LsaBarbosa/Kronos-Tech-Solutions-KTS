package com.kts.kronos.adapter.in.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record LoginResponse(
        @Schema(
                nullable = true,
                description = "Campo legado. Permanece nulo quando a sessão é estabelecida via cookie HttpOnly (Set-Cookie).",
                deprecated = true
        )
        String token
) {
    public static LoginResponse empty() {
        return new LoginResponse(null);
    }
}
