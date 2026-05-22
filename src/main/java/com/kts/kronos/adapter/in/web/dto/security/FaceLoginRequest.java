package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaceLoginRequest(
        @NotBlank(message = "A imagem da face é obrigatória")
        @Size(max = 1500000, message = "A imagem da face excede o tamanho máximo permitido.")
        String faceImageBase64,
        Boolean livenessPassed
) {
    @Override
    public String toString() {
        return "FaceLoginRequest[faceImageBase64=***MASKED***, livenessPassed=" + livenessPassed + "]";
    }
}