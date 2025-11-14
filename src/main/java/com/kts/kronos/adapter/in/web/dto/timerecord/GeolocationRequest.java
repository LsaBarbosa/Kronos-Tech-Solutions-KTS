package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotBlank;

import static com.kts.kronos.constants.Messages.IMAGE_DATA_NOT_BLANK;

public record GeolocationRequest(double latitude,
                                 double longitude,
                                 @NotBlank(message = IMAGE_DATA_NOT_BLANK)
                                 String faceImageBase64
                                 ) {
}
