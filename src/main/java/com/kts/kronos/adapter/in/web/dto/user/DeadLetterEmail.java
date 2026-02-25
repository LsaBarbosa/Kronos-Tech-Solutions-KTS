package com.kts.kronos.adapter.in.web.dto.user;

import com.kts.kronos.adapter.in.web.dto.event.PasswordRecoveryEmailRequestedEvent;

import java.time.LocalDateTime;

public record DeadLetterEmail(
        PasswordRecoveryEmailRequestedEvent event,
        String reason,
        LocalDateTime failedAt
) {
}
