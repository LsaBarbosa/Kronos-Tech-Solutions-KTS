package com.kts.kronos.observability.domain;

import java.time.OffsetDateTime;

public record ObservabilityStatus(
        String application,
        String status,
        String environment,
        OffsetDateTime timestamp
) {
}
