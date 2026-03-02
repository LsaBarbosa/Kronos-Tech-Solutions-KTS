package com.kts.kronos.adapter.in.web.dto.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.Document;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.DATE_TIME_PATTERN;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        @JsonFormat(pattern = DATE_TIME_PATTERN)
        LocalDateTime uploadedAt
) {



        public static DocumentResponse fromDomain(Document doc) {
                return new DocumentResponse(
                        doc.documentId(),
                        doc.fileName(),
                        doc.contentType(),
                        doc.uploadedAt()
                );
        }
}
