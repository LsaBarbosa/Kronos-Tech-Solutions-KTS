package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Document(
        UUID documentId,
        String fileName,
        String contentType,
        byte[] data,
        LocalDateTime uploadeAt,
        UUID employeeId,
        DocumentType type
) {
    public Document(String fileName, String contentType, byte[] data, LocalDateTime uploadeAt, UUID employeeId,DocumentType type) {
        this(
                UUID.randomUUID(), fileName, contentType, data, uploadeAt, employeeId, type);
    }

}
