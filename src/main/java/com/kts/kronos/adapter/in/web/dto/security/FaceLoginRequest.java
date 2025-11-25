package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;

public record FaceLoginRequest(
        @NotBlank(message = "A imagem da face é obrigatória")
        String faceImageBase64
) {}