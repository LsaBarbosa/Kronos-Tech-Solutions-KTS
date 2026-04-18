package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.IMAGE_DATA_NOT_BLANK;

public record GeolocationRequest(double latitude,
                                 double longitude,
                                 @NotBlank(message = IMAGE_DATA_NOT_BLANK)
                                 @Size(max = 1500000, message = "A imagem da face excede o tamanho máximo permitido.")
                                 String faceImageBase64,
                                 Boolean livenessPassed
) {
}