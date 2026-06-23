package com.kts.kronos.adapter.in.web.dto.demo;

import java.util.UUID;

public record DemoPurgeResponse(
        UUID operationId,
        String status,
        DemoOperationCounters removed,
        DemoValidationResult validation
) {}
