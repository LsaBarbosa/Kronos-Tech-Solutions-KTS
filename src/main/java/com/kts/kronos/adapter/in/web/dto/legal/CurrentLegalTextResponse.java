package com.kts.kronos.adapter.in.web.dto.legal;

import com.kts.kronos.domain.model.enuns.DocumentType;

public record CurrentLegalTextResponse(
        DocumentType type,
        String version,
        String title,
        String content,
        String contentHashSha256,
        boolean active
) {
}
