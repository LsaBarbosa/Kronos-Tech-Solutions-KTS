package com.kts.kronos.adapter.in.web.dto.security;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record AuthContext(UUID targetEmployeeId, UUID loggedEmployeeId, boolean isManagerView) {}
