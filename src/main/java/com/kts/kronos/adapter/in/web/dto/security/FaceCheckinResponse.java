package com.kts.kronos.adapter.in.web.dto.security;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record FaceCheckinResponse(
        String loginMessage,
        String recordMessage,
        String actionType,
        int autoLogoutAfterSeconds,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime recordedAt
) {
}
