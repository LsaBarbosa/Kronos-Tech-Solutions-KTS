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
        LocalDateTime uploadeAt,
        Long timeRecordId,
        boolean deletedByEmployee,
        boolean deletedByManager,
        String checksumSha256
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
                timeRecordId,
                deletedByEmployee,
                deletedByManager,
                null);
    }

    public Document(UUID employeeId, DocumentType type, String fileName, String contentType, String storagePath, LocalDateTime uploadeAt, Long timeRecordId,  boolean deletedByEmployee,
                    boolean deletedByManager, String checksumSha256) {
        this(
                UUID.randomUUID(),
                employeeId,
                type,
                fileName,
                contentType,
                storagePath,
                uploadeAt,
                timeRecordId,
                deletedByEmployee,
                deletedByManager,
                checksumSha256);
    }

    public Document(UUID documentId, UUID employeeId, DocumentType type, String fileName, String contentType, String storagePath, LocalDateTime uploadeAt,
                    Long timeRecordId, boolean deletedByEmployee, boolean deletedByManager) {
        this(
                documentId,
                employeeId,
                type,
                fileName,
                contentType,
                storagePath,
                uploadeAt,
                timeRecordId,
                deletedByEmployee,
                deletedByManager,
                null
        );
    }

    public Document markDeletedByEmployee() {
        return new Document(documentId, employeeId, type, fileName, contentType, storagePath, uploadeAt, timeRecordId, true, deletedByManager, checksumSha256);
    }

    // Método auxiliar para "marcar" como deletado pelo manager
    public Document markDeletedByManager() {
        return new Document(documentId, employeeId, type, fileName, contentType, storagePath, uploadeAt, timeRecordId, deletedByEmployee, true, checksumSha256);
    }
}
