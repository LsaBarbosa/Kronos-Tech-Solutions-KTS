package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição contendo dados de geolocalização e biometria facial do funcionário")
public record GeolocationRequest(
        @Schema(description = "Latitude registrada pelo dispositivo", example = "-22.9068")
        double latitude,

        @Schema(description = "Longitude registrada pelo dispositivo", example = "-43.1729")
        double longitude,

        @Schema(description = "Imagem facial capturada convertida em Base64", example = "iVBORw0KGgoAAAANSUhEUgAAAAE...")
        @NotBlank(message = "A imagem facial é obrigatória.")
        String faceImageBase64
) {}