package com.kts.kronos.adapter.in.web.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record PasswordRecoveryEmailRequestedEvent(
        UUID userId,
        String username,
        String toEmail,
        String resetToken,
        String frontendUrl,
        LocalDateTime requestedAt
) {
}
