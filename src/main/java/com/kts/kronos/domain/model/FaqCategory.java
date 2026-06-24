package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record FaqCategory(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
