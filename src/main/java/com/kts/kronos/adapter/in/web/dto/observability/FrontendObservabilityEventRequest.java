package com.kts.kronos.adapter.in.web.dto.observability;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrontendObservabilityEventRequest(
        @NotBlank
        @Size(max = 64)
        String eventType,

        @Size(max = 32)
        String level,

        @Size(max = 32)
        String category,

        @Size(max = 160)
        String route,

        @Size(max = 64)
        String source,

        @Size(max = 32)
        String result,

        @Size(max = 64)
        String reason,

        @Size(max = 240)
        String message,

        @Size(max = 128)
        String correlationId,

        @Min(0)
        @Max(120000)
        Long durationMs
) {
}
