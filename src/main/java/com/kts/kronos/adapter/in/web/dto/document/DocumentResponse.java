package com.kts.kronos.adapter.in.web.dto.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.Document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        @JsonFormat(pattern = "dd-MM-yy 'T' HH:mm:ss")
        LocalDateTime uploadedAt
) {
        public static DocumentResponse fromDomain(Document doc) {
                return new DocumentResponse(
                        doc.documentId(),
                        doc.fileName(),
                        doc.contentType(),
                        doc.uploadeAt()
                );
        }
}
