package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FaqArticle(
        UUID id,
        String title,
        String shortAnswer,
        String fullAnswer,
        FaqStatus status,
        int priority,
        FaqCategory category,
        List<Role> allowedRoles,
        List<String> screenKeys,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
