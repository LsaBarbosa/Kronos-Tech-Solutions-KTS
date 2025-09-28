package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record Document(
        UUID documentId,
        UUID employeeId,
        DocumentType type,
        String fileName,
        String contentType,
        String storagePath,
        LocalDateTime uploadeAt
) {
    public Document(UUID documentId, UUID employeeId, DocumentType type, String fileName, String contentType, String storagePath, LocalDateTime uploadeAt) {
        this.documentId = documentId;
        this.employeeId = employeeId;
        this.type = type;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.uploadeAt = uploadeAt;
    }

    // Construtor para criação de novo documento (chamado do Service)
    public Document(UUID employeeId, DocumentType type, String fileName, String contentType, String storagePath, LocalDateTime uploadeAt) {
        this(
                UUID.randomUUID(),
                employeeId,
                type,
                fileName,
                contentType,
                storagePath,
                uploadeAt);
    }

}