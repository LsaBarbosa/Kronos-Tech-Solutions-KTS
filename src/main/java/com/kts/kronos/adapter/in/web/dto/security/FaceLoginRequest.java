package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;

import static com.kts.kronos.constants.Messages.FACE_IMAGE_REQUIRED;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record FaceLoginRequest(
        @NotBlank(message = FACE_IMAGE_REQUIRED)
        String faceImageBase64
) {}
