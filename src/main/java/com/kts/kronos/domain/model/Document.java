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
        byte[] data,
        LocalDateTime uploadeAt
) {
    // Construtor secundário ajustado para a nova ordem, facilitando a criação do objeto
    public Document(UUID employeeId, DocumentType type, String fileName, String contentType, byte[] data, LocalDateTime uploadeAt) {
        this(
                UUID.randomUUID(),
                employeeId,
                type,
                fileName,
                contentType,
                data,
                uploadeAt);
    }

}
