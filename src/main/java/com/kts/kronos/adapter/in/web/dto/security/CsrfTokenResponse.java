package com.kts.kronos.adapter.in.web.dto.security;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
}
