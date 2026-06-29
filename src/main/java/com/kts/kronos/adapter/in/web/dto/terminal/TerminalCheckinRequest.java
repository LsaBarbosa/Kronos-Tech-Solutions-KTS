package com.kts.kronos.adapter.in.web.dto.terminal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TerminalCheckinRequest(
        @NotBlank(message = "A imagem da face é obrigatória.")
        @Size(max = 1500000, message = "A imagem da face excede o tamanho máximo permitido.")
        String faceImageBase64,
        Boolean livenessPassed,
        double latitude,
        double longitude
) {
    @Override
    public String toString() {
        return "TerminalCheckinRequest{faceImageBase64=[REDACTED], livenessPassed=" + livenessPassed
                + ", latitude=" + latitude + ", longitude=" + longitude + "}";
    }
}
