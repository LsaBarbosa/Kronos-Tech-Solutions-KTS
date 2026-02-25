package com.kts.kronos.adapter.in.web.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordRecoveryEmailRequestedEvent(
        UUID userId,
        String username,
        String toEmail,
        String resetToken,
        String frontendUrl,
        LocalDateTime requestedAt
) {
}