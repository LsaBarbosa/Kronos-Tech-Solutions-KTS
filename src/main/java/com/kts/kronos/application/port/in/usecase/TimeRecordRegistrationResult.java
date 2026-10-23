package com.kts.kronos.application.port.in.usecase;

import java.time.OffsetDateTime;

public record TimeRecordRegistrationResult(
        String message,
        String actionType,
        OffsetDateTime recordedAt
) {
}
