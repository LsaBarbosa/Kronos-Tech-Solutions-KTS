package com.kts.kronos.adapter.in.web.dto.user;

import com.kts.kronos.adapter.in.web.dto.event.PasswordRecoveryEmailRequestedEvent;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record DeadLetterEmail(
        PasswordRecoveryEmailRequestedEvent event,
        String reason,
        LocalDateTime failedAt
) {
}
