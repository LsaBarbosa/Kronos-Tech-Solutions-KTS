package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
