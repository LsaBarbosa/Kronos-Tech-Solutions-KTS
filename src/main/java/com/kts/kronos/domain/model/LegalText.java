package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record LegalText(
        UUID legalTextId,
        DocumentType documentType,
        String version,
        String title,
        String content,
        String contentHashSha256,
        boolean active,
        Instant createdAt,
        Instant publishedAt
) {
}
