package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FaceCheckinRequest(
        @NotBlank(message = "A imagem da face é obrigatória")
        @Size(max = 1500000, message = "A imagem da face excede o tamanho máximo permitido.")
        String faceImageBase64,
        @NotNull(message = "Latitude é obrigatória")
        Double latitude,
        @NotNull(message = "Longitude é obrigatória")
        Double longitude,
        Double accuracy,
        Boolean livenessPassed
) {
    @Override
    public String toString() {
        return "FaceCheckinRequest[faceImageBase64=***MASKED***, latitude=" + latitude
                + ", longitude=" + longitude
                + ", accuracy=" + accuracy
                + ", livenessPassed=" + livenessPassed + "]";
    }
}
