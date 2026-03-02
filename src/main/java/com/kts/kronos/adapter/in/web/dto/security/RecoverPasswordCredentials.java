package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.User;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record RecoverPasswordCredentials(UUID userId, String username, String employeeEmail) {}
