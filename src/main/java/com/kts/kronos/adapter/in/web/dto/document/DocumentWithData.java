package com.kts.kronos.adapter.in.web.dto.document;

import com.kts.kronos.domain.model.enuns.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record DocumentWithData(UUID documentId,
                               UUID employeeId,
                               DocumentType type,
                               String fileName,
                               String contentType,
                               byte[] data,
                               LocalDateTime uploadedAt
) {}
