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
        LocalDateTime uploadedAt,
        Long timeRecordId,
        boolean deletedByEmployee,
        boolean deletedByManager
) {
  // Construtor para criação de novo documento (chamado do Service)
    public Document(UUID employeeId, DocumentType type, String fileName, String contentType, String storagePath, LocalDateTime uploadeAt, Long timeRecordId,  boolean deletedByEmployee,
                    boolean deletedByManager) {
        this(
                UUID.randomUUID(),
                employeeId,
                type,
                fileName,
                contentType,
                storagePath,
                uploadeAt,
                timeRecordId,deletedByEmployee,deletedByManager);
    }

    public Document markDeletedByEmployee() {
        return new Document(documentId, employeeId, type, fileName, contentType, storagePath, uploadedAt, timeRecordId, true, deletedByManager);
    }

    // Método auxiliar para "marcar" como deletado pelo manager
    public Document markDeletedByManager() {
        return new Document(documentId, employeeId, type, fileName, contentType, storagePath, uploadedAt, timeRecordId, deletedByEmployee, true);
    }
}