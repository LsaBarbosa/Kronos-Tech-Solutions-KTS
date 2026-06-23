package com.kts.kronos.adapter.in.web.dto.demo;

import java.time.LocalDateTime;

public record DemoStatusResponse(
        boolean enabled,
        boolean killSwitch,
        boolean exists,
        String companyName,
        String username,
        String sandboxKey,
        LastOperation lastOperation,
        DemoValidationResult validation
) {
    public record LastOperation(
            String operation,
            String status,
            LocalDateTime finishedAt
    ) {}
}
