package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;

import static com.kts.kronos.constants.Messages.FACE_IMAGE_REQUIRED;

public record FaceLoginRequest(
        @NotBlank(message = FACE_IMAGE_REQUIRED)
        String faceImageBase64
) {}