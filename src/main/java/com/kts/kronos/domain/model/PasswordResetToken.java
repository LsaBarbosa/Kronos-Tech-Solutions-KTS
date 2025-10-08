package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
public record PasswordResetToken(
        String token,
        UUID userId,
        LocalDateTime expiryDate,
        LocalDateTime createdAt
) {
}