package com.kts.kronos.adapter.in.web.dto.platform;

public record PlatformHealthSignalResponse(
        String id,
        String label,
        String state,
        String description
) {
}
